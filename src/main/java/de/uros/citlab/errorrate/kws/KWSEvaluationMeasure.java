/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.kws;

import de.uros.citlab.errorrate.kws.measures.IRankingMeasure;
import de.uros.citlab.errorrate.kws.measures.IRankingStatistic;
import de.uros.citlab.errorrate.types.KWS;
import de.uros.citlab.errorrate.types.KWS.GroundTruth;
import de.uros.citlab.errorrate.types.KWS.Line;
import de.uros.citlab.errorrate.types.KWS.Page;
import de.uros.citlab.errorrate.types.KWS.Word;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author tobias
 */
public class KWSEvaluationMeasure {

    private static Logger LOG = LoggerFactory.getLogger(KWSEvaluationMeasure.class);

    public interface KeyWordMatcher {
        void initPage(List<KWS.Entry> polyRefs);

        /**
         * value between 0.0 = no match and 1.0 = perfect match.
         * If only binary decision can be done, use 0.0 and 1.0.
         * Of value == 0.0 no match is done.
         * If value > 0.0 a match is assumed (but with low matching confidence)
         *
         * @param gt
         * @param hyp
         * @return
         */
        double matches(KWS.Entry gt, KWS.Entry hyp);

    }


    //    public String getStats() {
//        if (meanStats == null && globalStats == null) {
//            getGlobalMearsure();
//            return globalStats.toString();
//        }
//        if (meanStats == null) {
//            return globalStats.toString();
//        } else {
//            return meanStats.toString();
//        }
    public static Map<IRankingMeasure.Measure, Double> getMeasure(KWS.Result hypo, KWS.GroundTruth ref, KeyWordMatcher matcher, IRankingMeasure.Measure... ms) {
        return getMeasure(hypo, ref, matcher, Arrays.asList(ms));
    }
//    }

    public static Map<IRankingMeasure.Measure, Double> getMeasure(KWS.Result hypo, KWS.GroundTruth ref, KeyWordMatcher matcher, Collection<IRankingMeasure.Measure> ms) {
        List<KWS.MatchList> matchLists = getMatchList(hypo, ref, matcher);
        HashMap<IRankingMeasure.Measure, Double> ret = new HashMap<>();
        for (IRankingMeasure.Measure m : ms) {
            ret.put(m, m.getMethod().calcMeasure(matchLists));
        }
        return ret;
    }

    public static Map<IRankingStatistic.Statistic, double[]> getStats(KWS.Result hypo, KWS.GroundTruth ref, KeyWordMatcher matcher, Collection<IRankingStatistic.Statistic> ss) {
        List<KWS.MatchList> matchLists = getMatchList(hypo, ref, matcher);
        HashMap<IRankingStatistic.Statistic, double[]> ret = new HashMap<>();
        for (IRankingStatistic.Statistic s : ss) {
            ret.put(s, s.getMethod().calcStatistic(matchLists));
        }
        return ret;

    }

    static List<KWS.MatchList> getMatchList(KWS.Result hypo, KWS.GroundTruth ref, KeyWordMatcher matcher) {
        List<Pair<KWS.Word, KWS.Word>> kewordPair = alignWords(hypo.getKeywords(), ref);
        LinkedList<KWS.MatchList> matchLists = new LinkedList<>();
//        HashMap<String, Page> refPagewise = new HashMap<>();
//        for (Page page : ref.getPages()) {
//            refPagewise.put(page.getPageID(), page);
//        }
        for (Pair<KWS.Word, KWS.Word> pair : kewordPair) {
            KWS.Word refs = pair.getSecond();
            KWS.Word hypos = pair.getFirst();
            KWS.MatchList matchList = new KWS.MatchList(hypos, refs, matcher);
            matchLists.add(matchList);
            LOG.trace("for keyword '{}' found {} gt and {} hyp", refs.getKeyWord(), refs.getPos().size(), hypos.getPos().size());
        }
        return matchLists;
    }

    /**
     * aligns words. The result is a list of all reco-ref-pairs of words. the KWS.Word is never null, but the inner list could be empty.
     *
     * @param keywords_hypo
     * @param keywords_ref
     * @return
     */
    private static List<Pair<KWS.Word, KWS.Word>> alignWords(Set<Word> keywords_hypo, GroundTruth keywords_ref) {
        HashMap<String, KWS.Word> wordsHyp = generateMap(keywords_hypo);
        HashMap<String, KWS.Word> wordsRef = generateMap(keywords_ref);

        Set<String> queryWords = new HashSet<>();
        queryWords.addAll(wordsHyp.keySet());
        queryWords.addAll(wordsRef.keySet());

        LinkedList<Pair<KWS.Word, KWS.Word>> ret = new LinkedList<>();
        for (String queryWord : queryWords) {
            Word wordRef = wordsRef.get(queryWord);
            Word wordHyp = wordsHyp.get(queryWord);
            if (wordHyp == null) {
                wordHyp = new Word(queryWord);
            }
            if (wordRef == null) {
                wordRef = new Word(queryWord);
            }
            ret.add(new Pair<>(wordHyp, wordRef));
        }
        return ret;
    }

    private static HashMap<String, KWS.Word> generateMap(Set<KWS.Word> keywords) {
        HashMap<String, KWS.Word> words = new HashMap<>();
        for (KWS.Word kwsWord : keywords) {
            words.put(kwsWord.getKeyWord(), kwsWord);
        }
        return words;
    }

    private static HashMap<String, KWS.Word> generateMap(GroundTruth keywords_ref) {
        HashMap<String, KWS.Word> keyword2KWSWordMap = new HashMap<>();
        for (Page page : keywords_ref.getPages()) {
            for (Line line : page.getLines()) {
                for (String keyword : line.getKeyword2Baseline().keySet()) {
                    List<Polygon> baselines = line.getKeyword2Baseline().get(keyword);
                    List<Polygon> polys = line.getKeyword2Polygons().get(keyword);
                    KWS.Word word = keyword2KWSWordMap.get(keyword);
                    if (word == null) {
                        word = new Word(keyword);
                        keyword2KWSWordMap.put(keyword, word);
                    }
                    for (int i = 0; i < baselines.size(); i++) {
                        KWS.Entry ent = new KWS.Entry(
                                Double.NaN,
                                line.getLineID(),
                                page.getPageID(),
                                baselines == null || baselines.isEmpty() ? null : baselines.get(i),
                                polys==null || polys.isEmpty() ? null : polys.get(i));
                        ent.setParentLine(line);
                        word.add(ent);
                    }
                }
            }
        }
        LOG.info("groundtruth map has {} keywords", keyword2KWSWordMap.size());
        return keyword2KWSWordMap;
    }

//    public static void main(String[] args) {
//        GroundTruth gt = new GroundTruth();
//        Page page = new Page("page1");
//        Line line = new Line("", null, null);
//        line.addKeyword("AA", PolygonUtil.string2Polygon("0,0 1,1"), null);
//        line.addKeyword("AA", PolygonUtil.string2Polygon("0,0 2,2"), null);
//        line.addKeyword("BB", PolygonUtil.string2Polygon("1,1 2,2"), null);
//        page.addLine(line);
//        gt.addPages(page);
//        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
//        System.out.println(gson.toJson(gt));
//
//    }
}
