/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.kws;

import de.uros.citlab.errorrate.interfaces.ILine;
import de.uros.citlab.errorrate.types.KWS;
import de.uros.citlab.errorrate.util.ExtractUtil;
import de.uros.citlab.errorrate.util.PolygonUtil;
import de.uros.citlab.tokenizer.interfaces.ITokenizer;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author gundram
 */
public class KeywordExtractor {

    private HashMap<String, Pattern> keywords = new LinkedHashMap<>();
    private final boolean part;
    private final boolean upper;
    private int maxSize = 1000;
    private final String prefix = "([^\\pL\\pN\\pM\\p{Cs}\\p{Co}])";
    private final String suffix = "([^\\pL\\pN\\pM\\p{Cs}\\p{Co}])";
    private final Pattern prefixPattern = Pattern.compile("^" + prefix);
    private final Pattern suffixPattern = Pattern.compile(suffix + "$");

    public KeywordExtractor() {
        this(false, false);
    }

    public KeywordExtractor(boolean part, boolean upper) {
        this.part = part;
        this.upper = upper;
    }

    public interface Page {
        List<ILine> getLines();

        String getID();
    }

    /**
     * for a given text line provide possible keyword (which must not be in the text line).
     * Two default implementation are available  {@link KeywordExtractor.FixedKeyWordProvider} and {@link KeywordExtractor.TokenKeyWordProvider}
     */
    public interface KeyWordProvider {
        Set<String> getKeywords(String textLine);
    }

    /**
     * return the given keywords independent of the text line
     */
    public static class FixedKeyWordProvider implements KeyWordProvider {
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
    public static class TokenKeyWordProvider implements KeyWordProvider {
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
        if (upper) {
            kw = kw.toUpperCase();
        }
        Pattern res = keywords.get(kw);
        if (res == null) {
            res = part ? Pattern.compile(kw) : Pattern.compile("((^)|" + prefix + ")" + kw + "(($)|" + suffix + ")");
            if (keywords.size() > maxSize) {
                keywords.clear();
            }
            keywords.put(kw, res);
        }
        return res;
    }

    public double[][] getKeywordPosition(String keyword, String line) {
        if (upper) {
            line = line.toUpperCase();
        }
        Pattern p = getPattern(keyword);
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

    public KWS.GroundTruth getKeywordGroundTruth(PageIterator iterator, KeyWordProvider keyWordProvider) {
        List<KWS.Page> pages = new LinkedList<>();
        iterator.getIterator().forEachRemaining(page -> pages.add(getKeywordsFromPage(page, keyWordProvider)));
        return new KWS.GroundTruth(pages);
    }

    public KWS.Page getKeywordsFromPage(Page page, KeyWordProvider keyWordProvider) {
        List<ILine> lines = page.getLines();
        KWS.Page pageRes = new KWS.Page(page.getID());
        for (ILine line : lines) {
            KWS.Line kwsLine = new KWS.Line(line.getBaseline());
            pageRes.addLine(kwsLine);
            String textline = upper ? line.getText().toUpperCase() : line.getText();
            Set<String> tokenize = keyWordProvider.getKeywords(textline);
            for (String keyword : tokenize) {
                double[][] keywordPosition = getKeywordPosition(keyword, textline);
                for (double[] ds : keywordPosition) {
                    kwsLine.addKeyword(keyword, PolygonUtil.getPolygonPart(line.getBaseline(), ds[0], ds[1]));
                }
            }
        }
        return pageRes;
    }

}
