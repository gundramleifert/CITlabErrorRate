package de.uros.citlab.errorrate.util;

import de.uros.citlab.errorrate.kws.KeywordExtractor;
import de.uros.citlab.errorrate.kws.QueryConfig;
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


    public static class QueryAndResult {
        public QueryAndResult(Collection<String> query, KWS.GroundTruth result) {
            this.query = query;
            this.result = result;
        }

        public Collection<String> getQuery() {
            return query;
        }

        public KWS.GroundTruth getResult() {
            return result;
        }

        private Collection<String> query;
        private KWS.GroundTruth result;
    }

    public static ObjectCounter<String> getCandidates(KWS.GroundTruth keywordGroundTruth) {
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

    public static List<String> createKwsQuery(KeywordExtractor.PageIterator pi, ITokenizer tokenizer, QueryConfig config) {
        KeywordExtractor kwe = new KeywordExtractor();

        //count words in pages, that are provided
        KWS.GroundTruth keywordGroundTruth = kwe.getKeywordGroundTruth(pi, new KeywordExtractor.TokenKeyWordProvider(tokenizer));
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

    public static QueryAndResult createKwsResult(KeywordExtractor.PageIterator pi, Collection<String> query, QueryConfig config) {
        KeywordExtractor kwe = new KeywordExtractor(config);
        KeywordExtractor.KeyWordProvider wkp = new KeywordExtractor.FixedKeyWordProvider(query);
        KWS.GroundTruth keywordGroundTruth = kwe.getKeywordGroundTruth(pi, wkp);
        ObjectCounter<String> queryCandidates = getCandidates(keywordGroundTruth);
        //filter from all possible queries all which match the constraint
        List<Pair<String, Long>> resultOccurrence = queryCandidates.getResultOccurrence();
        resultOccurrence.removeIf(entry ->
                entry.getFirst().length() < config.getMinLen()
                        || (config.getMaxLen() > 0 && entry.getFirst().length() > config.getMaxLen())
                        || (config.isAlpha() && !isAlpha(entry.getFirst()))
                        || entry.getSecond() < config.getMinOcc()
                        || (config.getMaxOcc() > 0 && entry.getFirst().length() > config.getMaxOcc())
        );

        final LinkedList<String> queries = new LinkedList<>();
        resultOccurrence.forEach(stringLongPair -> queries.add(stringLongPair.getFirst()));
        LOG.debug("take {}/{} queries", queries.size(), resultOccurrence.size());
        return new QueryAndResult(query, keywordGroundTruth);
    }
}

