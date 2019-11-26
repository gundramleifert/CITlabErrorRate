package de.uros.citlab.errorrate.kws.measures;

import de.uros.citlab.errorrate.types.KWS;

import java.util.LinkedList;
import java.util.List;

public class AverageMatcherConfidence implements IRankingMeasure {
    @Override
    public double calcMeasure(List<KWS.MatchList> matchlists) {
        LinkedList<KWS.Match> list = new LinkedList<>();
        double sum = 0;
        int anz = 0;
        for (KWS.MatchList matchList : matchlists) {
            for (KWS.Match match : matchList.matches) {
                double matchConf = match.getMatchConf();
                if (!Double.isNaN(matchConf)) {
                    sum += matchConf;
                    anz++;
                }
            }
        }
        return sum / anz;
    }

}
