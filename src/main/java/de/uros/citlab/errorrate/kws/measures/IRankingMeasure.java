/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.kws.measures;

import de.uros.citlab.errorrate.types.KWS;
import java.util.List;

/**
 *
 * @author tobias
 */
public interface IRankingMeasure {

    enum Measure {
        MAP(new MeanAveragePrecision()),
        WMAP(new WMeanAveragePrecision()),
        GAP(new GlobalAveragePrecision()),
        PRECISION(new Precision()),
        RECALL(new Recall()),
        R_PRECISION(new RPrecision()),
        G_NCDG(new GNDCG()),
        M_NCDG(new MNDCG()),
        PRECISION_AT_10(new PrecisionAt10()),
        AVERAGE_MATCHER_CONFIDENCE(new AverageMatcherConfidence());

        private IRankingMeasure method;

        Measure(IRankingMeasure method) {
            this.method = method;
        }

        public IRankingMeasure getMethod() {
            return method;
        }

    }

    public double calcMeasure(List<KWS.MatchList> matchlists);

}
