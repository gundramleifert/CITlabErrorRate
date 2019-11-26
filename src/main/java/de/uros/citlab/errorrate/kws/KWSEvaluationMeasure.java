/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.kws;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.uros.citlab.errorrate.kws.measures.IRankingMeasure;
import de.uros.citlab.errorrate.kws.measures.IRankingStatistic;
import de.uros.citlab.errorrate.types.KWS;
import de.uros.citlab.errorrate.types.KWS.GroundTruth;
import de.uros.citlab.errorrate.types.KWS.Line;
import de.uros.citlab.errorrate.types.KWS.Page;
import de.uros.citlab.errorrate.types.KWS.Word;
import de.uros.citlab.errorrate.util.PolygonUtil;
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

    private final KeyWordMatcher matcher;
    //    private KWS.Result hypo;
//    private KWS.GroundTruth ref;
//    List<KWS.MatchList> matchLists;
    //    private double thresh = 0.5;
//    private double toleranceDefault = 20.0;
//    private boolean useLineBaseline = true;
    private static Logger LOG = LoggerFactory.getLogger(KWSEvaluationMeasure.class);
//    private MatchObserver mo = null;

    public KWSEvaluationMeasure(KeyWordMatcher matcher) {
        this.matcher = matcher;
    }

//    public void setResults(KWS.Result hypo) {
//        this.hypo = hypo;
////        matchLists = null;
//    }

//    public void setMatchObserver(MatchObserver mo) {
//        this.mo = mo;
//    }

//    public static interface MatchObserver {
//
//        public void evalMatch(KWS.MatchList list);
//    }

    public interface KeyWordMatcher {
        void initPage(List<KWS.Entry> polyRefs);

        /**
         * value between 0 = no match and 1 = match.
         * If only binary decision can be done, use 0.0 and 1.0.
         *
         * @param gt
         * @param hyp
         * @return
         */
        double matches(KWS.Entry gt, KWS.Entry hyp);

    }

    public static class SameLineIDMatcher implements KeyWordMatcher {

        @Override
        public void initPage(List<KWS.Entry> polyRefs) {
        }

        @Override
        public double matches(KWS.Entry gt, KWS.Entry hyp) {
            String lineIdGt = gt.getLineID();
            if (lineIdGt == null || lineIdGt.isEmpty()) {
                throw new RuntimeException("lineID of gt is '" + lineIdGt + "'");
            }
            String lineIDHyp = hyp.getLineID();
            if (lineIDHyp == null || lineIDHyp.isEmpty()) {
                throw new RuntimeException("lineID of hyp is '" + lineIDHyp + "'");
            }
            return lineIdGt.equals(lineIDHyp) ? 1.0 : 0.0;
        }
    }

    public static class IntersectionOverUnionBBMatcher implements KeyWordMatcher {

        @Override
        public void initPage(List<KWS.Entry> polyRefs) {
        }

        @Override
        public double matches(KWS.Entry gt, KWS.Entry hyp) {
            Polygon polyGT = gt.getPoly();
            if (polyGT == null) {
                throw new RuntimeException("polygon of gt " + gt + " is null");
            }
            Polygon polyHyp = hyp.getPoly();
            if (polyHyp == null) {
                throw new RuntimeException("polygon of hyp " + hyp + " is null");
            }
            Rectangle boundsGT = polyGT.getBounds();
            Rectangle boundsHyp = polyHyp.getBounds();
            if (!boundsGT.intersects(boundsHyp)) {
                return 0.0;
            }
            Rectangle intersection = boundsGT.intersection(boundsHyp);
            Rectangle union = boundsGT.union(boundsHyp);
            return ((intersection.getHeight() * intersection.getWidth()) / (1.0 * union.getWidth() * union.getHeight()));
        }
    }

    public static class BaseLineKeyWordMatcher implements KeyWordMatcher {
        //        private final double thresh;
        private double toleranceDefault = 40.0;
        private List<KWS.Line> lineList = null;

        public BaseLineKeyWordMatcher() {
            this(0.5);
        }

        public BaseLineKeyWordMatcher(double toleranceDefault) {
            this.toleranceDefault = toleranceDefault;
        }

        @Override
        public void initPage(List<KWS.Entry> polyRefs) {
        }

        @Override
        public double matches(KWS.Entry gt, KWS.Entry hyp) {
            if (gt.getBaseLine() == null) {
                throw new RuntimeException("baseline of gt " + gt + " is null");
            }
            if (hyp.getBaseLine() == null) {
                throw new RuntimeException("baseline of hyp " + hyp + " is null");
            }
            double tol = toleranceDefault;
            Polygon toHit = PolygonUtil.thinOut(PolygonUtil.blowUp(gt.getBaseLine()), (int) Math.round((tol + 3) / 4));
            Polygon hypo = PolygonUtil.thinOut(PolygonUtil.blowUp(hyp.getBaseLine()), (int) Math.round((tol + 3) / 4));
            double cnt = 0.0;
            Rectangle toCntBB = toHit.getBounds();
            Rectangle hypoBB = hypo.getBounds();
            Rectangle inter = toCntBB.intersection(hypoBB);
            for (int i = 0; i < toHit.npoints; i++) {
                int xA = toHit.xpoints[i];
                int yA = toHit.ypoints[i];
                int minI = Math.min(inter.width, inter.height);
                //Early stopping criterion
                if (minI < -3.0 * tol) {
                    continue;
                }
                double minDist = Double.MAX_VALUE;
                for (int j = 0; j < hypo.npoints; j++) {
                    int xC = hypo.xpoints[j];
                    int yC = hypo.ypoints[j];
                    minDist = Math.min(Math.abs(xA - xC) + Math.abs(yA - yC), minDist);
//                    minDist = Math.min(Math.sqrt((xC - xA) * (xC - xA) + (yC - yA) * (yC - yA)), minDist);
                    if (minDist <= tol) {
                        break;
                    }
                }
                if (minDist <= tol) {
                    cnt++;
                }
                if (minDist > tol && minDist < 3.0 * tol) {
                    cnt += (3.0 * tol - minDist) / (2.0 * tol);
                }
            }
            cnt /= toHit.npoints;
            return cnt;
        }
    }


    public List<KWS.MatchList> getMatchList(KWS.Result hypo, KWS.GroundTruth ref) {
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
    public Map<IRankingMeasure.Measure, Double> getMeasure(KWS.Result hypo, KWS.GroundTruth ref, IRankingMeasure.Measure... ms) {
        return getMeasure(hypo, ref, Arrays.asList(ms));
    }
//    }

    public Map<IRankingMeasure.Measure, Double> getMeasure(KWS.Result hypo, KWS.GroundTruth ref, Collection<IRankingMeasure.Measure> ms) {
        List<KWS.MatchList> matchLists = getMatchList(hypo, ref);
        HashMap<IRankingMeasure.Measure, Double> ret = new HashMap<>();
        for (IRankingMeasure.Measure m : ms) {
            ret.put(m, m.getMethod().calcMeasure(matchLists));
        }
        return ret;
    }

    public Map<IRankingStatistic.Statistic, double[]> getStats(KWS.Result hypo, KWS.GroundTruth ref, Collection<IRankingStatistic.Statistic> ss) {
        List<KWS.MatchList> matchLists = getMatchList(hypo, ref);
        HashMap<IRankingStatistic.Statistic, double[]> ret = new HashMap<>();
        for (IRankingStatistic.Statistic s : ss) {
            ret.put(s, s.getMethod().calcStatistic(matchLists));
        }
        return ret;

    }

    /**
     * aligns words. The result is a list of all reco-ref-pairs of words. the KWS.Word is never null, but the inner list could be empty.
     *
     * @param keywords_hypo
     * @param keywords_ref
     * @return
     */
    private List<Pair<KWS.Word, KWS.Word>> alignWords(Set<Word> keywords_hypo, GroundTruth keywords_ref) {
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

    private HashMap<String, KWS.Word> generateMap(Set<KWS.Word> keywords) {
        HashMap<String, KWS.Word> words = new HashMap<>();
        for (KWS.Word kwsWord : keywords) {
            words.put(kwsWord.getKeyWord(), kwsWord);
        }
        return words;
    }

    private HashMap<String, KWS.Word> generateMap(GroundTruth keywords_ref) {
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
                                baselines.get(i),
                                polys == null ? null : polys.get(i));
                        ent.setParentLine(line);
                        word.add(ent);
                    }
                }
            }
        }
        LOG.info("groundtruth map has {} keywords", keyword2KWSWordMap.size());
        return keyword2KWSWordMap;
    }

    public static void main(String[] args) {
        GroundTruth gt = new GroundTruth();
        Page page = new Page("page1");
        Line line = new Line("", null, null);
        line.addKeyword("AA", PolygonUtil.string2Polygon("0,0 1,1"), null);
        line.addKeyword("AA", PolygonUtil.string2Polygon("0,0 2,2"), null);
        line.addKeyword("BB", PolygonUtil.string2Polygon("1,1 2,2"), null);
        page.addLine(line);
        gt.addPages(page);
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
        System.out.println(gson.toJson(gt));

    }
}
