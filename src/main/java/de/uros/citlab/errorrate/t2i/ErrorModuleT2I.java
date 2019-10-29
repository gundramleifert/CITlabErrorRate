package de.uros.citlab.errorrate.t2i;

import de.uros.citlab.errorrate.htr.end2end.ErrorModuleEnd2End;
import de.uros.citlab.errorrate.interfaces.IErrorModuleWithSegmentation;
import de.uros.citlab.errorrate.interfaces.ILine;
import de.uros.citlab.errorrate.interfaces.ILineComparison;
import de.uros.citlab.errorrate.interfaces.IPoint;
import de.uros.citlab.errorrate.types.Count;
import de.uros.citlab.errorrate.types.Metric;
import de.uros.citlab.errorrate.types.PathCalculatorGraph;
import de.uros.citlab.errorrate.util.ObjectCounter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

//import de.uros.citlab.errorrate.types.PathCalculatorGraph;

public class ErrorModuleT2I implements IErrorModuleWithSegmentation {

    private ErrorModuleEnd2End moduleRaw;
    int cor = 0;
    int hyp = 0;
    ObjectCounter<Count> countPrec = new ObjectCounter<Count>();
    ObjectCounter<Count> countRec = new ObjectCounter<Count>();

    public ErrorModuleT2I(boolean allowSegmentationErrors) {
        moduleRaw = new ErrorModuleEnd2End(false, true, allowSegmentationErrors, false);
    }

    @Override
    public void calculateWithSegmentation(List<? extends ILine> reco, List<? extends ILine> ref) {
        calculateWithSegmentation(reco, ref, true);
    }

    private static void addAll(ObjectCounter<Count> counter, List<IPoint> path) {
        for (IPoint iPoint : path) {
            switch (iPoint.getManipulation()) {
                case INS:
                    counter.add(Count.INS);
                    counter.add(Count.GT);
                    break;
                case DEL:
                    counter.add(Count.DEL);
                    counter.add(Count.HYP);
                    break;
                case SUB:
                    counter.add(Count.SUB);
                    counter.add(Count.HYP);
                    counter.add(Count.GT);
                    break;
                case COR:
                    counter.add(Count.COR);
                    counter.add(Count.HYP);
                    counter.add(Count.GT);
                    break;
                default:
                    throw new RuntimeException("cannot interprete " + iPoint.getManipulation());

            }
        }

    }

    @Override
    public List<ILineComparison> calculateWithSegmentation(List<? extends ILine> reco, List<? extends ILine> ref, boolean calcLineComparison) {
        List<ILineComparison> iLineComparisons1 = moduleRaw.calculateWithSegmentation(reco, ref, true);
        for (ILineComparison l : iLineComparisons1) {
            if (!l.getRefText().isEmpty()) {
                addAll(countRec, l.getPath());
            }
            if (!l.getRecoText().isEmpty()) {
                hyp++;
                addAll(countPrec, l.getPath());
                if (l.getRecoText().equals(l.getRefText())) {
                    cor++;
                }
            }

        }
        return iLineComparisons1;
    }

    @Override
    public List<ILineComparison> calculate(String reco, String ref, boolean calcLineComparison) {
        throw new UnsupportedOperationException("need baselines to compare");
    }

    @Override
    public List<ILineComparison> calculate(List<String> reco, List<String> ref, boolean calcLineComparison) {
        throw new UnsupportedOperationException("need baselines to compare");
    }

    @Override
    public Map<Metric, Double> getMetrics() {
        Map<Metric, Double> res = new HashMap<>();
        res.put(Metric.ERR,
                countPrec.get(Count.HYP) == 0 ?
                        0 :
                        (countPrec.get(Count.INS) +
                                countPrec.get(Count.DEL) +
                                countPrec.get(Count.SUB) + 0.0)
                                / countPrec.get(Count.HYP)
        );
        res.put(Metric.REC,
                countRec.get(Count.GT) == 0 ?
                        1 :
                        (countRec.get(Count.COR) + 0.0)
                                / countRec.get(Count.GT)
        );
        res.put(Metric.PREC, hyp == 0 ? 1D : (1.0 * cor) / hyp);
        return res;
    }

    @Override
    public void calculate(String reco, String ref) {
        throw new UnsupportedOperationException("need baselines to compare");
    }

    @Override
    public void calculate(List<String> reco, List<String> ref) {
        throw new UnsupportedOperationException("need baselines to compare");
    }

    @Override
    public List<String> getResults() {
        List<String> res = new LinkedList<>();
        Map<Metric, Double> metrics = getMetrics();
        for (Metric metric : metrics.keySet()) {
            res.add(metric.toString() + "=" + metrics.get(metric));
        }
        return res;
    }

    @Override
    public ObjectCounter<Count> getCounter() {
        return null;
    }

    @Override
    public void reset() {
        cor = 0;
        hyp = 0;
        countPrec.reset();
        countRec.reset();
        moduleRaw.reset();
    }
}
