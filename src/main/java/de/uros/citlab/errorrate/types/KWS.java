/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.types;

import com.google.gson.annotations.Expose;
import de.uros.citlab.errorrate.kws.KWSEvaluationMeasure;
import de.uros.citlab.errorrate.util.ObjectCounter;
import de.uros.citlab.errorrate.util.PolygonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author gundram
 */
public class KWS {

    public static class MatchList {

        public List<Match> matches;
        public ObjectCounter<Type> counter = new ObjectCounter<>();
        private static Logger LOG = LoggerFactory.getLogger(MatchList.class);
//    private final IBaseLineAligner aligner;

        public MatchList(List<Match> matches) {
            this.matches = matches;
            for (Match match : matches) {
                counter.add(match.type);
            }
        }

        //    private MatchList(IBaseLineAligner matcher) {
//        this.matcher = matcher;
//    }
        public MatchList(Word hypos, Word refs, KWSEvaluationMeasure.KeyWordMatcher matcher) {
            this(match(hypos, refs, matcher));
        }

        private int getCount(Type type) {
            return (int) counter.get(type);
        }

        public int getRefSize() {
            return getCount(Type.TRUE_POSITIVE) + getCount(Type.FALSE_NEGATIVE);
        }

        public int getHypSize() {
            return getCount(Type.TRUE_POSITIVE) + getCount(Type.FALSE_POSITIVE);
        }

        private static LinkedList<Match> getMatchList(String word, List<KWS.Entry> gts, List<KWS.Entry> hyps, KWSEvaluationMeasure.KeyWordMatcher matcher) {
            LinkedList<Match> intermediate = new LinkedList<>();
            if (gts != null && !gts.isEmpty() && hyps != null && !hyps.isEmpty()) {
                //maybe sum matches between lines
                for (Entry aGT : gts) {
                    for (Entry aHYP : hyps) {
                        double matchesConfidence = matcher.matches(aHYP, aGT);
                        if (matchesConfidence > 0.0) {
                            intermediate.add(new Match(Type.TRUE_POSITIVE, matchesConfidence, word, aHYP, aGT));
                        }
                    }
                }
                //sort by confidence
                intermediate.sort((o1, o2) -> Double.compare(o2.getMatchConf(), o1.getMatchConf()));
            }

            //ensure, that each gt and hyp can only be matched once
            LinkedList<Match> res = new LinkedList<>();
            Set<Entry> gtsRemain = new LinkedHashSet<>();
            if (gts != null) {
                gtsRemain.addAll(gts);
            }
            Set<Entry> hypsRemain = new LinkedHashSet<>();
            if (hyps != null) {
                hypsRemain.addAll(hyps);
            }
            while (!intermediate.isEmpty()) {
                Match first = intermediate.getFirst();
                Entry gt = first.getGT();
                Entry hyp = first.getHyp();
                gtsRemain.remove(gt);
                hypsRemain.remove(hyp);
                res.add(first);
                intermediate.removeIf(match -> match.getHyp() == first.getHyp() || match.getGT() == first.getGT());
            }
            for (Entry entry : gtsRemain) {
                res.add(new Match(Type.FALSE_NEGATIVE, Double.NaN, word, null, entry));
            }
            for (Entry entry : hypsRemain) {
                res.add(new Match(Type.FALSE_POSITIVE, Double.NaN, word, entry, null));
            }
            Collections.sort(res);
            return res;
        }


        private static final List<Match> match(Word hypos, Word refs, KWSEvaluationMeasure.KeyWordMatcher matcher) {

            String keyWord = hypos.getKeyWord();

            HashMap<String, List<KWS.Entry>> page2PolyHypos = generatePloys(hypos);
            HashMap<String, List<KWS.Entry>> page2PolyRefs = generatePloys(refs);
//            HashMap<String, List<Double>> page2Tolerance = getTolerances(refs);
//        HashMap<String, List<Polygon>> page2AllBaselines = getAllLines(ref);
            LinkedList<Match> ret = new LinkedList<>();

            //collect all pageIDs from hypos and refs
            Set<String> pages = new HashSet<>(page2PolyRefs.keySet());
            pages.addAll(page2PolyHypos.keySet());

            //iterate over all pages
            for (String pageID : pages) {
//            List<Polygon> allLines = page2AllBaselines.get(pageID);
                List<KWS.Entry> polyHypos = page2PolyHypos.get(pageID);
                List<KWS.Entry> polyRefs = page2PolyRefs.get(pageID);
//                List<Double> toleranceRefs = page2Tolerance.get(pageID);
                ret.addAll(getMatchList(keyWord, polyRefs, polyHypos, matcher));
            }
            return ret;

        }

        public void sort() {
            Collections.sort(matches);
        }

        private static HashMap<String, List<KWS.Entry>> generatePloys(Word kwsWord) {
            HashMap<String, List<KWS.Entry>> ret = new HashMap<>();
            List<KWS.Entry> poss = kwsWord.getPos();
            Collections.sort(poss, new Comparator<KWS.Entry>() {
                        @Override
                        public int compare(KWS.Entry o1, KWS.Entry o2) {
                            return -Double.compare(o1.getConf(), o2.getConf());
                        }
                    }
            );
            for (KWS.Entry pos : poss) {
                String pageID = pos.getPageID();
                List<KWS.Entry> get = ret.get(pageID);
                if (get == null) {
                    get = new LinkedList<>();
                    ret.put(pageID, get);
                }
                get.add(pos);

            }
            return ret;
        }

        private static HashMap<String, List<Polygon>> getAllLines(GroundTruth ref) {
            HashMap<String, List<Polygon>> ret = new HashMap<>();
            for (Page page : ref.getPages()) {
                LinkedList<Polygon> pagePolys = new LinkedList<>();
                ret.put(page.getPageID(), pagePolys);
                for (Line line : page.getLines()) {
                    pagePolys.add(line.getBaseline());
                }
            }
            return ret;
        }
    }

    public enum Type {
        TRUE_POSITIVE, FALSE_NEGATIVE, FALSE_POSITIVE
    }

    public static class Match implements Comparable<Match> {

        public final Type type;
        public final double matchConf;

        private final KWS.Entry hyp;
        private final KWS.Entry gt;


        private String word;

        //        public Match(Type type, Entry entry, String word) {
//            this(type, entry.getConf(), entry.getBaseLineKeyword(), entry.getBaseLineLine(), entry.getImage(), word);
//        }
        public Match(Type type, double matchConf, String word, KWS.Entry hyp, KWS.Entry gt) {
            this.type = type;
            this.matchConf = matchConf;
            this.word = word;
            this.hyp = hyp;
            this.gt = gt;
        }

        public double getHypConfidence() {
            return getHyp() == null ? 0.0 : getHyp().getConf();
        }

        @Override
        public int compareTo(Match o) {
            return Double.compare(o.getHypConfidence(), getHypConfidence());
        }

        public String getWord() {
            return word;
        }

        @Override
        public String toString() {
            return "KwsMatch{hyp=" + hyp + ",gt=" + gt + ", type=" + type + ", conf=" + matchConf + '}';
        }

        public KWS.Entry getGT() {
            return gt;
        }

        public KWS.Entry getHyp() {
            return hyp;
        }

        public double getMatchConf() {
            return matchConf;
        }

    }

    public static class Line {

        @Expose
        private Map<String, List<String>> kws = new TreeMap<>();
        @Expose
        private Map<String, List<String>> kwsP = new TreeMap<>();
        private transient HashMap<String, List<Polygon>> kwsL = new HashMap<>();
        private transient HashMap<String, List<Polygon>> kwsLP = new HashMap<>();

        //    @Expose
//    private Page parent;
        @Expose
        private String bl = null;
        private transient Polygon blL;

        @Expose
        private String poly = null;
        private transient Polygon polyL;
        //        private transient double tolerance;
        @Expose
        private String lineID;

        public Line(String lineID, Polygon bl, Polygon poly) {
            this.blL = bl;
            this.polyL = poly;
            this.lineID = lineID;
            this.bl = PolygonUtil.polygon2String(bl);
            this.poly = PolygonUtil.polygon2String(poly);
        }

        public String getLineID() {
            return lineID;
        }
//        public double getTolerance() {
//            return tolerance;
//        }
//
//        public void setTolerance(double tolerance) {
//            this.tolerance = tolerance;
//        }

        //    public Line(Page parent) {
//        this.parent = parent;
//    }
        public void addKeyword(String word, Polygon bl, Polygon poly) {
            List<Polygon> getL = kwsL.get(word);
            List<String> get = kws.get(word);
            List<Polygon> getLP = kwsLP.get(word);
            List<String> getP = kwsP.get(word);
            if (getL == null) {
                getL = new LinkedList<>();
                kwsL.put(word, getL);
                get = new LinkedList<>();
                kws.put(word, get);
                getLP = new LinkedList<>();
                kwsLP.put(word, getLP);
                getP = new LinkedList<>();
                kwsP.put(word, getP);
            }
            getL.add(bl);
            get.add(PolygonUtil.polygon2String(bl));
            getLP.add(poly);
            getP.add(PolygonUtil.polygon2String(poly));
        }

        public void removeKeyword(String kw) {
            kwsL.remove(kw);
            kws.remove(kw);
            kwsLP.remove(kw);
            kwsP.remove(kw);
        }

        public HashMap<String, List<Polygon>> getKeyword2Baseline() {
            if (kwsL == null) {
                kwsL = new HashMap<>();
                if (kws == null) {
                    return kwsL;
                }
                for (String keyword : kws.keySet()) {
                    LinkedList<Polygon> polyL = new LinkedList<>();
                    kwsL.put(keyword, polyL);
                    for (String poly : kws.get(keyword)) {
                        polyL.add(PolygonUtil.string2Polygon(poly));
                    }
                }
            }
            return kwsL;
        }

        public HashMap<String, List<Polygon>> getKeyword2Polygons() {
            if (kwsLP == null) {
                kwsLP = new HashMap<>();
                if (kwsP == null) {
                    return kwsLP;
                }
                for (String keyword : kwsP.keySet()) {
                    LinkedList<Polygon> polyLP = new LinkedList<>();
                    kwsLP.put(keyword, polyLP);
                    for (String poly : kwsP.get(keyword)) {
                        polyLP.add(PolygonUtil.string2Polygon(poly));
                    }
                }
            }
            return kwsLP;
        }

        //    public Page getParent() {
//        return parent;
//    }
        public Polygon getBaseline() {
            if (blL == null && bl != null) {
                blL = PolygonUtil.string2Polygon(bl);
            }
            return blL;
        }

        public Polygon getPolygon() {
            if (polyL == null && poly != null) {
                polyL = PolygonUtil.string2Polygon(poly);
            }
            return polyL;
        }
    }

    public static class GroundTruth {

        @Expose
        private List<Page> pages;

        public GroundTruth() {
            pages = new LinkedList<>();
        }

        public GroundTruth(List<Page> pages) {
            this.pages = pages;
        }

        public void addPages(Page page) {
            pages.add(page);
        }

        public List<Page> getPages() {
            return pages;
        }

        public int getRefCount() {
            int cnt = 0;
            for (Page page : pages) {
                for (Line line : page.getLines()) {
                    for (List<String> value : line.kws.values()) {
                        cnt += value.size();
                    }
                }
            }
            return cnt;
        }

    }

    public static class Entry implements Comparable<Entry> {

        @Expose
        private double conf;
        @Expose
        private String bl;
        @Expose
        private String poly;
        @Expose
        private String line;
        @Expose
        private String image;
        private transient Polygon blL;
        private transient Polygon polyL;

        private transient Line parentLine;

        public void setParentLine(Line parentLine) {
            this.parentLine = parentLine;
        }

        public Line getParentLine() {
            return parentLine;
        }

//        public Entry(double conf, String lineID, Polygon bl, String pageId) {
//            this(conf, lineID, array2String(bl.xpoints, bl.ypoints, bl.npoints), pageId);
//            this.poly = bl;
//        }

        public Entry(double conf, String lineID, String pageId, Polygon bl, Polygon poly) {
            this.conf = conf;
            this.line = lineID;
            this.image = pageId;
            this.blL = bl;
            this.bl = PolygonUtil.polygon2String(bl);
            this.polyL = poly;
            this.poly = PolygonUtil.polygon2String(poly);
        }

        public Polygon getBaseLine() {
            if (blL == null) {
                blL = PolygonUtil.string2Polygon(bl);
            }
            return blL;
        }

        public Polygon getPoly() {
            if (polyL == null) {
                polyL = PolygonUtil.string2Polygon(poly);
            }
            return polyL;
        }

        public Polygon getBaseLineOfTextLine() {
            return parentLine == null ? null : parentLine.getBaseline();
        }

        public double getConf() {
            return conf;
        }

        //    public String getId() {
//        return id;
//    }
        public String getPageID() {
            return image;
        }

        public String getLineID() {
            return line;
        }

        public String getBl() {
            return bl;
        }

        @Override
        public String toString() {
            return "Entry{" + "conf=" + conf + ", bl=" + bl + ", lineID=" + line + ", pageID=" + image + '}';
        }

        @Override
        public int compareTo(Entry o) {
            return Double.compare(o.conf, conf);
        }

    }

    public static class Page {

        @Expose
        private String pageID;
        @Expose
        private List<Line> lines = new LinkedList<>();

        public Page(String pageId) {
            this.pageID = pageId;
        }

        /**
         * @param pageId
         * @param lines
         */
        public Page(String pageId, List<Line> lines) {
            this.pageID = pageId;
            this.lines = lines;
        }

        public List<Line> getLines() {
            return lines;
        }

        public String getPageID() {
            return pageID;
        }

        public void addLine(Line line) {
            lines.add(line);
        }

    }

    public static class Result {

        @Expose
        private Set<Word> keywords;

        @Expose
        private Long time;

        public Result(Set<Word> keywords, Long totalTime) {
            this.keywords = keywords;
            this.time = totalTime;
        }

        public Result(Set<Word> keywords) {
            this(keywords, null);
        }

        public Set<Word> getKeywords() {
            return keywords;
        }

        public Long getTotalTime() {
            return time;
        }

        public void setTotalTime(Long totalTime) {
            this.time = totalTime;
        }

        public void append(Result resPart) {
            time += resPart.getTotalTime();
            for (Word wordPart : resPart.getKeywords()) {
                if (keywords.contains(wordPart)) {
                    String keyWord = wordPart.getKeyWord();
                    for (Word word : keywords) {
                        if (word.getKeyWord().equals(keyWord)) {
                            word.addAll(wordPart.getPos());
                            break;
                        }
                        word.time += wordPart.getTime();
                    }
                } else {
                    keywords.add(wordPart);
                }
            }
        }

    }

    public static class Word {

        @Expose
        private String kw;
        @Expose
        LinkedList<Entry> pos = new LinkedList<>();
        @Expose
        private Long time = 0L;

        private int maxSize = -1;
        private double minConf = Double.MAX_VALUE;
        private boolean isSorted = false;
        private ObjectCounter<String> oc = new ObjectCounter<>();

        public Word(String kw, int maxSize, double minConf) {
            this(kw, maxSize);
            this.minConf = minConf;
        }

        public Word(String kw, int maxSize) {
            this.kw = kw;
            this.maxSize = maxSize;
        }

        public double getMinConf() {
            return minConf;
        }

        public void addCount(String key) {
            oc.add(key);
        }

        public ObjectCounter<String> getStatistic() {
            return oc;
        }

        public boolean addAll(Collection<? extends Entry> c) {
            for (Entry entry : c) {
                add(entry);
            }
            return true;
        }

        public Word(String kw) {
            this.kw = kw;
        }

        public String getKeyWord() {
            return kw;
        }

        public Long getTime() {
            return time;
        }

        public void setTime(Long time) {
            this.time = time;
        }

        public void addTime(Long time) {
            this.time += time;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public synchronized void add(KWS.Entry entry) {
            if (maxSize <= 0 || pos.size() < maxSize) {
                pos.add(entry);
                isSorted = false;
//            Collections.sort(pos);
                return;
            }
            if (!isSorted) {
                Collections.sort(pos);
                isSorted = true;
            }
            if (pos.getLast().getConf() < entry.getConf()) {
                pos.removeLast();
                pos.add(entry);
                Collections.sort(pos);
                isSorted = true;
//            System.out.println(minConf + " -> " + entry.getConf());
                minConf = entry.getConf();
            }
        }

        public int size() {
            return pos.size();
        }

        public List<KWS.Entry> getPos() {
            return pos;
        }

        @Override
        public String toString() {
            return "KeyWord{" + "kw=" + kw + ", entries=" + pos + '}';
        }

    }

}
