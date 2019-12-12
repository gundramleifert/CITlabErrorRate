package de.uros.citlab.errorrate.kws;

import de.uros.citlab.errorrate.kws.KWSEvaluationMeasure.KeyWordMatcher;
import de.uros.citlab.errorrate.types.KWS;
import de.uros.citlab.errorrate.util.PolygonUtil;

import java.awt.*;
import java.util.List;


public class KeyWordMatchers {

//    public static KeyWordMatcher matchBaselines()

    public static KeyWordMatcher sameLineID() {
        return new SameLineIDMatcher();
    }

    public static KeyWordMatcher nearBaselines(double distance) {
        return new BaseLineKeyWordMatcher(distance);
    }

    public static KeyWordMatcher nearBaselines() {
        return new BaseLineKeyWordMatcher();
    }


    public static KeyWordMatcher intersectionOverUnionBoundingBoxes() {
        return new IntersectionOverUnionBBMatcher();
    }

    public static KeyWordMatcher withThreshold(final double thresh, KeyWordMatcher matcher) {

        return new KeyWordMatcher() {
            @Override
            public void initPage(List<KWS.Entry> polyRefs) {
                matcher.initPage(polyRefs);
            }

            @Override
            public double matches(KWS.Entry gt, KWS.Entry hyp) {
                double matches = matcher.matches(gt, hyp);
                return matches < thresh ? 0.0 : matches;
            }
        };
    }

    private static class SameLineIDMatcher implements KeyWordMatcher {


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

    private static class IntersectionOverUnionBBMatcher implements KeyWordMatcher {

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

    private static class BaseLineKeyWordMatcher implements KeyWordMatcher {
        private final double toleranceDefault;

        public BaseLineKeyWordMatcher() {
            this(40.0);
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


}
