package de.uros.citlab.errorrate.util;

import de.uros.citlab.errorrate.kws.KeywordExtractor;
import de.uros.citlab.errorrate.types.KWS;
import de.uros.citlab.tokenizer.interfaces.ITokenizer;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author gundram
 */
public class KwsUtil {

    private static Logger LOG = LoggerFactory.getLogger(KwsUtil.class);
    //        @ParamAnnotation(descr="folder with xml-files")
    private String xml_in;

    //        @ParamAnnotation(descr="folder with image-files (if empty use index of xml-files")
    private String img_in;

    //        @ParamAnnotation(descr="path to output groundtruth file")
    private String o;

    //        @ParamAnnotation(descr="path to input query file (if empty, take all keywords from xml-file")
    private String q_in;

    //        @ParamAnnotation(descr="path to output query file (if empty, no query file saved")
    private String q_out;

    //        @ParamAnnotation(descr="minimal length of keyword")
    private int minlen;

    //        @ParamAnnotation(descr="maximal length of keyword")
    private int maxlen;

    //        @ParamAnnotation(descr="minimal occurance of keyword")
    private int minocc;

    //        @ParamAnnotation(descr="maximal occurance of keyword (<0 for any)")
    private int maxocc;

    //        @ParamAnnotation(descr="take only apha-channels (ignore numerical)")
    private boolean alpha;

    //        @ParamAnnotation(descr="make everything upper")
    private boolean upper;

    //        @ParamAnnotation(descr="find keyword also as part of longer words (substring)")
    private boolean part;

    public KwsUtil(int minlen, int maxlen, int minocc, int maxocc, boolean alpha, boolean upper, boolean part) {
        this.minlen = minlen;
        this.maxlen = maxlen;
        this.minocc = minocc;
        this.maxocc = maxocc;
        this.alpha = alpha;
        this.upper = upper;
        this.part = part;
    }

    public KwsUtil() {
        this(1, -1, 1, -1, false, false, false);
    }

    public class QueryAndResult {
        public QueryAndResult(Collection<String> query, KWS.GroundTruth result) {
            this.query = query;
            this.result = result;
        }

        public Collection<String> query;
        public KWS.GroundTruth result;
    }

    public ObjectCounter<String> getCandidates(KWS.GroundTruth keywordGroundTruth) {
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

    public QueryAndResult createKwsQueryAndResult(KeywordExtractor.PageIterator pi, ITokenizer tokenizer) {
        KeywordExtractor kwe = new KeywordExtractor(false, upper);

        //count words in pages, that are provided
        KWS.GroundTruth keywordGroundTruth = kwe.getKeywordGroundTruth(pi, new KeywordExtractor.TokenKeyWordProvider(tokenizer));
        ObjectCounter<String> queryCandidates = getCandidates(keywordGroundTruth);
        //filter from all possible queries all which match the constraint
        List<Pair<String, Long>> resultOccurrence = queryCandidates.getResultOccurrence();
        resultOccurrence.removeIf(entry ->
                        entry.getFirst().length() < minlen
                                || (maxlen > 0 && entry.getFirst().length() > maxlen)
                                || (alpha && !isAlpha(entry.getFirst()))
//                        skip this, because specific tokens can match in second part (e.g. part=true)
//                        || entry.getSecond() < minocc
//                        || (maxocc > 0 && entry.getFirst().length() > maxocc)
        );
        //extract queries
        final LinkedList<String> queries = new LinkedList<>();
        resultOccurrence.forEach(stringLongPair -> queries.add(stringLongPair.getFirst()));
        LOG.debug("in first round take {}/{} queries", queries.size(), resultOccurrence.size());
        return createKwsResult(pi, queries);
    }

    public QueryAndResult createKwsResult(KeywordExtractor.PageIterator pi, Collection<String> query) {
        KeywordExtractor kwe = new KeywordExtractor(part, upper);
        KeywordExtractor.KeyWordProvider wkp = new KeywordExtractor.FixedKeyWordProvider(query);
        KWS.GroundTruth keywordGroundTruth = kwe.getKeywordGroundTruth(pi, wkp);
        ObjectCounter<String> queryCandidates = getCandidates(keywordGroundTruth);
        //filter from all possible queries all which match the constraint
        List<Pair<String, Long>> resultOccurrence = queryCandidates.getResultOccurrence();
        resultOccurrence.removeIf(entry ->
                entry.getFirst().length() < minlen
                        || (maxlen > 0 && entry.getFirst().length() > maxlen)
                        || (alpha && !isAlpha(entry.getFirst()))
                        || entry.getSecond() < minocc
                        || (maxocc > 0 && entry.getFirst().length() > maxocc)
        );

        final LinkedList<String> queries = new LinkedList<>();
        resultOccurrence.forEach(stringLongPair -> queries.add(stringLongPair.getFirst()));
        LOG.debug("take {}/{} queries", queries.size(), resultOccurrence.size());
        return new QueryAndResult(query, keywordGroundTruth);
    }
}

