package de.uros.citlab.errorrate.kws;

import de.uros.citlab.errorrate.interfaces.ILine;
import de.uros.citlab.errorrate.kws.measures.IRankingMeasure;
import de.uros.citlab.errorrate.types.KWS;
import org.junit.Test;

import java.awt.*;
import java.util.List;
import java.util.*;

public class RealLiveTest {

    @Test
    public void TestAll() {
        Polygon polygon1 = new Polygon(new int[]{0, 1000}, new int[]{100, 100}, 2);
        Polygon polygon2 = new Polygon(new int[]{0, 1000}, new int[]{150, 150}, 2);
        Polygon polygon3 = new Polygon(new int[]{0, 1000}, new int[]{200, 200}, 2);
        KeywordExtractor tweKeywordExtractor = new KeywordExtractor();
        Set<String> queries = new HashSet<>(Arrays.asList("Salat"));
        KWS.GroundTruth keywordGroundTruth = tweKeywordExtractor.getKeywordGroundTruth(
                new KeywordExtractor.PageIterator() {
                    @Override
                    public Iterator<KeywordExtractor.Page> getIterator() {
                        List<KeywordExtractor.Page> pages = new LinkedList<>();
                        pages.add(new KeywordExtractor.Page() {
                                      @Override
                                      public List<ILine> getLines() {
                                          List<ILine> lines = new LinkedList<>();
                                          lines.add(new ILine() {
                                              @Override
                                              public String getText() {
                                                  return "Heringssalat rocks";
                                              }

                                              @Override
                                              public Polygon getBaseline() {
                                                  return polygon1;
                                              }

                                              @Override
                                              public String getId() {
                                                  return "ID_1";
                                              }
                                          });
                                          lines.add(new ILine() {
                                              @Override
                                              public String getText() {
                                                  return "more than Salat.";
                                              }

                                              @Override
                                              public Polygon getBaseline() {
                                                  return polygon2;
                                              }

                                              @Override
                                              public String getId() {
                                                  return "ID_2";
                                              }

                                          });
                                          return lines;
                                      }

                                      @Override
                                      public String getID() {
                                          return "pageID_1";
                                      }
                                  }
                        );
                        pages.add(new KeywordExtractor.Page() {
                                      @Override
                                      public List<ILine> getLines() {
                                          List<ILine> lines = new LinkedList<>();
                                          lines.add(new ILine() {
                                              @Override
                                              public String getText() {
                                                  return "salat1234 rocks";
                                              }

                                              @Override
                                              public Polygon getBaseline() {
                                                  return polygon1;
                                              }

                                              @Override
                                              public String getId() {
                                                  return "ID_1";
                                              }

                                          });
                                          lines.add(new ILine() {
                                              @Override
                                              public String getText() {
                                                  return "not so much";
                                              }

                                              @Override
                                              public Polygon getBaseline() {
                                                  return polygon2;
                                              }

                                              @Override
                                              public String getId() {
                                                  return "ID_2";
                                              }

                                          });
                                          return lines;
                                      }

                                      @Override
                                      public String getID() {
                                          return "pageID_1";
                                      }
                                  }
                        );
                        return pages.iterator();
                    }
                },
                new KeywordExtractor.KeyWordProvider() {
                    @Override
                    public Set<String> getKeywords(String textLine) {
                        return queries;
                    }
                }
        );
        KWS.Word word = new KWS.Word("salat");
        word.add(new KWS.Entry(0.9, "ID_1", "pageID_1", null, polygon1));
        word.add(new KWS.Entry(0.9, "ID_2", "pageID_1", null, polygon2));
        word.add(new KWS.Entry(0.9, "ID_1", "pageID_2", null, polygon1));
        word.add(new KWS.Entry(0.9, "ID_2", "pageID_2", null, polygon2));

        KWSEvaluationMeasure measure = new KWSEvaluationMeasure(new KWSEvaluationMeasure.BaseLineKeyWordMatcher());
        Map<IRankingMeasure.Measure, Double> measure1 = measure.getMeasure(
                new KWS.Result(new HashSet<>(Arrays.asList(word))),
                keywordGroundTruth,
                IRankingMeasure.Measure.PRECISION);
    }
}
