/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.kws;

import de.uros.citlab.errorrate.interfaces.ILine;
import de.uros.citlab.errorrate.types.KWS;
import de.uros.citlab.errorrate.util.ExtractUtil;
import de.uros.citlab.errorrate.util.ObjectCounter;
import de.uros.citlab.errorrate.util.PolygonUtil;
import de.uros.citlab.tokenizer.interfaces.ITokenizer;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author gundram
 */
public class KeywordExtractor {
    final static Logger LOG = LoggerFactory.getLogger(KeywordExtractor.class);
    private HashMap<String, Pattern> keywords = new LinkedHashMap<>();
    QueryConfig config;
    private int maxSize = -1;
    private final String prefix = "([^\\pL\\pN\\pM\\p{Cs}\\p{Co}])";
    private final String suffix = "([^\\pL\\pN\\pM\\p{Cs}\\p{Co}])";
    private final Pattern prefixPattern = Pattern.compile("^" + prefix);
    private final Pattern suffixPattern = Pattern.compile(suffix + "$");

    public KeywordExtractor() {
        this(new QueryConfig.Builder(false, false).build());
    }

    public KeywordExtractor(QueryConfig config) {
        this.config = config;
    }

    public interface Page {
        List<ILine> getLines();

        String getID();
    }

    /**
     * for a given text line provide possible keyword (which must not be in the text line).
     * Two default implementation are available  {@link KeywordExtractor.FixedKeyWordProvider} and {@link KeywordExtractor.TokenKeyWordProvider}
     */
    private interface KeyWordProvider {
        Set<String> getKeywords(String textLine);
    }

    /**
     * return the given keywords independent of the text line
     */
    private static class FixedKeyWordProvider implements KeyWordProvider {
        private final Set<String> keywords;

        public FixedKeyWordProvider(Collection<String> keywords) {
            this.keywords = new LinkedHashSet<>(keywords);
        }

        @Override
        public Set<String> getKeywords(String textLine) {
            return keywords;
        }
    }

    /**
     * returns all tokens which are returned by the tokenizer on a given text line.
     */
    private static class TokenKeyWordProvider implements KeyWordProvider {
        private final ITokenizer tokenizer;

        public TokenKeyWordProvider(ITokenizer tokenizer) {
            this.tokenizer = tokenizer;
        }

        @Override
        public Set<String> getKeywords(String textLine) {
            return new LinkedHashSet<>(tokenizer.tokenize(textLine));
        }
    }

    /**
     * A PageIterator has to provide a page. Each page has to contain all text lines on the given pages.
     * The pageID has to be unique, whereby the LineID hast to be unique within the page.
     */
    public interface PageIterator {
        Iterator<Page> getIterator();
    }

    /**
     * creates an iterator over xmls which are saved in pageXML-format.
     * If no IDs are given, the path of the xml-files is taken as id.
     */
    public static class FileListPageIterator implements PageIterator {
        Iterator<Page> res;

        public FileListPageIterator(String[] filePaths) {
            this(filePaths, filePaths);
        }

        public FileListPageIterator(String[] filePaths, String[] fileIds) {
            List<Page> list = new LinkedList<>();
            for (int i = 0; i < filePaths.length; i++) {
                final List<ILine> lines = ExtractUtil.getLinesFromFile(new File(filePaths[i]));
                final int j = i;
                list.add(new Page() {
                    @Override
                    public List<ILine> getLines() {
                        return lines;
                    }

                    @Override
                    public String getID() {
                        return fileIds == null ? String.valueOf(j) : fileIds[j];
                    }
                });
            }
            res = list.iterator();
        }

        @Override
        public Iterator<Page> getIterator() {
            return res;
        }
    }

    private Pattern getPattern(String kw) {
        if (config.isUpper()) {
            kw = kw.toUpperCase();
        }
        Pattern res = keywords.get(kw);
        if (res == null) {
            String quote = Pattern.quote(kw);
            if (!quote.equals(kw)) {
                LOG.debug("escape characters of keyword - change '{}' to '{}'. ", kw, quote);
            }
            res = config.isPart() ? Pattern.compile(kw) : Pattern.compile("((^)|" + prefix + ")" + quote + "(($)|" + suffix + ")");
            if (maxSize < 0 || keywords.size() <= maxSize) {
                keywords.put(kw, res);
            }
        }
        return res;
    }

    public double[][] getKeywordPositions(String keyword, String line) {
        if (config.isUpper()) {
            line = line.toUpperCase();
        }
        String quoted = Pattern.quote(keyword);
        if (!quoted.equals(keyword)) {
            LOG.debug("escape characters in keyword from '{}' to '{}'.", keyword, quoted);
        }
        Pattern p = getPattern(quoted);
        Matcher matcher = p.matcher(line);
        int idx = 0;
        List<double[]> startEnd = new LinkedList<>();
        while (matcher.find(idx)) {
            idx = matcher.start() + 1;
            String group = matcher.group();
            Matcher matcherPrefix = prefixPattern.matcher(group);
            Matcher matcherSuffix = suffixPattern.matcher(group);
            double[] match = new double[]{
                    (matcherPrefix.find() ? matcher.start() + matcherPrefix.group().length() : matcher.start()) / ((double) line.length()),
                    (matcherSuffix.find() ? matcher.end() - matcherSuffix.group().length() : matcher.end()) / ((double) line.length())
            };
            startEnd.add(match);
        }
        return startEnd.toArray(new double[0][]);
    }

    public KWS.GroundTruth getKeywordGroundTruth(PageIterator iterator, Collection<String> keywords) {
        KeywordExtractor.KeyWordProvider kp = new KeywordExtractor.FixedKeyWordProvider(keywords);
        List<KWS.Page> pages = new LinkedList<>();
        iterator.getIterator().forEachRemaining(page -> pages.add(getKeyWordGroundTruthFromPage(page, kp)));
        return new KWS.GroundTruth(pages);
    }

    private KWS.GroundTruth getKeywordGroundTruth(PageIterator iterator, ITokenizer keywordExtractor) {
        KeywordExtractor.KeyWordProvider kp = new KeywordExtractor.TokenKeyWordProvider(keywordExtractor);
        List<KWS.Page> pages = new LinkedList<>();
        iterator.getIterator().forEachRemaining(page -> pages.add(getKeyWordGroundTruthFromPage(page, kp)));
        return new KWS.GroundTruth(pages);
    }

    private KWS.Page getKeyWordGroundTruthFromPage(Page page, KeyWordProvider keyWordProvider) {
        List<ILine> lines = page.getLines();
        KWS.Page pageRes = new KWS.Page(page.getID());
        for (ILine line : lines) {
            KWS.Line kwsLine = new KWS.Line(line.getId(), line.getBaseline(), line.getPolygon());
            pageRes.addLine(kwsLine);
            String textLine = config.isUpper() ? line.getText().toUpperCase() : line.getText();
            Set<String> keywords = keyWordProvider.getKeywords(textLine);
            for (String keyword : keywords) {
                String keywordNormalized = config.isUpper() ? keyword.toUpperCase() : keyword;
                double[][] keywordPosition = getKeywordPositions(keywordNormalized, textLine);
                for (double[] ds : keywordPosition) {
                    kwsLine.addKeyword(keywordNormalized, PolygonUtil.getPolygonPart(line.getBaseline(), ds[0], ds[1]), null);
                }
            }
        }
        return pageRes;
    }

    public static class QueryAndResult {
        public QueryAndResult(Collection<String> query, KWS.GroundTruth result) {
            this.query = query;
            this.result = result;
        }

        private Collection<String> query;
        private KWS.GroundTruth result;

        public Collection<String> getQuery() {
            return query;
        }

        public KWS.GroundTruth getResult() {
            return result;
        }

    }

    private static ObjectCounter<String> getCandidates(KWS.GroundTruth keywordGroundTruth) {
        ObjectCounter<String> queryCandidates = new ObjectCounter<>();
        for (KWS.Page page : keywordGroundTruth.getPages()) {
            for (KWS.Line line : page.getLines()) {
                for (Map.Entry<String, List<Polygon>> entry : line.getKeyword2Baseline().entrySet()) {
                    queryCandidates.add(entry.getKey(), entry.getValue().size());
                }
            }
        }
        return queryCandidates;
    }

    private static boolean isAlpha(String str) {
        for (char c : str.toCharArray()) {
            if (!Character.isAlphabetic(c)) {
                return false;
            }
        }
        return true;
    }

    private static List<String> createKwsQuery(PageIterator pi, ITokenizer tokenizer, QueryConfig config) {
        KeywordExtractor kwe = new KeywordExtractor();

        //count words in pages, that are provided
        KWS.GroundTruth keywordGroundTruth = kwe.getKeywordGroundTruth(pi, tokenizer);
        ObjectCounter<String> queryCandidates = getCandidates(keywordGroundTruth);
        //filter from all possible queries all which match the constraint
        List<Pair<String, Long>> resultOccurrence = queryCandidates.getResultOccurrence();
        resultOccurrence.removeIf(entry ->
                        entry.getFirst().length() < config.getMinLen()
                                || (config.getMaxLen() > 0 && entry.getFirst().length() > config.getMaxLen())
                                || (config.isAlpha() && !isAlpha(entry.getFirst()))
//                        skip this, because specific tokens can match in second part (e.g. part=true)
//                        || entry.getSecond() < minocc
//                        || (maxocc > 0 && entry.getFirst().length() > maxocc)
        );
        //extract queries
        LinkedList<String> queries = new LinkedList<>();
        resultOccurrence.forEach(stringLongPair -> queries.add(stringLongPair.getFirst()));
        LOG.debug("in first round take {}/{} queries", queries.size(), resultOccurrence.size());
        return queries;
//        return createKwsResult(pi, queries,config);
    }

    public static QueryAndResult createQueryAndResult(KeywordExtractor.PageIterator pi, ITokenizer tokenizer, QueryConfig config) {
        List<String> kwsQueryIntermediate = createKwsQuery(pi, tokenizer, config);
        return createQueryAndResult(pi, kwsQueryIntermediate, config);
    }

    public static QueryAndResult createQueryAndResult(KeywordExtractor.PageIterator pi, Collection<String> query, QueryConfig config) {
        KeywordExtractor kwe = new KeywordExtractor(config);
        KWS.GroundTruth keywordGroundTruth = kwe.getKeywordGroundTruth(pi, query);
        ObjectCounter<String> queryCandidates = getCandidates(keywordGroundTruth);
        //filter from all possible queries all which match the constraint
        List<Pair<String, Long>> resultOccurrence = queryCandidates.getResultOccurrence();
        int resBefore = resultOccurrence.size();
        resultOccurrence.removeIf(entry ->
                entry.getFirst().length() < config.getMinLen()
                        || (config.getMaxLen() > 0 && entry.getFirst().length() > config.getMaxLen())
                        || (config.isAlpha() && !isAlpha(entry.getFirst()))
                        || entry.getSecond() < config.getMinOcc()
                        || (config.getMaxOcc() > 0 && entry.getFirst().length() > config.getMaxOcc())
        );

        final LinkedList<String> queries = new LinkedList<>();
        resultOccurrence.forEach(stringLongPair -> queries.add(stringLongPair.getFirst()));
        LOG.debug("take {}/{} queries", queries.size(), resBefore);
        if (queries.size() < query.size()) {
            if (LOG.isWarnEnabled()) {
                LinkedList<String> removed = new LinkedList<>(query);
                removed.removeAll(queries);
                LOG.warn("delete {} keywords because they do not fulfill the configuration {}: {} ", removed.size(), config, removed);
            }
        }
        return new QueryAndResult(queries, keywordGroundTruth);
    }

}
