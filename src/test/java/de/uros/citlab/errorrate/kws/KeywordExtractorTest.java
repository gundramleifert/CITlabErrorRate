/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.kws;

import de.uros.citlab.errorrate.kws.measures.IRankingMeasure;
import de.uros.citlab.errorrate.types.KWS;
import org.apache.commons.io.FileUtils;
import org.junit.*;

import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.*;

import static org.junit.Assert.assertTrue;

/**
 * @author gundram
 */
public class KeywordExtractorTest {

    private static final File folderGT = new File("src/test/resources/gt");
    private static final File folderBot = new File("src/test/resources/hyp_bot");
    private static final File folderErr = new File("src/test/resources/hyp_err");

    private static File[] listGT;
    private static File[] listBot;
    private static File[] listErr;

    public KeywordExtractorTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        cases.add(new TestCase("the the they athe the", "the", 3, 5));
        cases.add(new TestCase("abab ab -da", "ab", 1, 3));
        cases.add(new TestCase("wadde hadde du de da", "du de", 1, 1));
    }

    @BeforeClass
    public static void setUpFolder() {
        listGT = setUpFolder(folderGT);
        listErr = setUpFolder(folderErr);
        listBot = setUpFolder(folderBot);
    }

    private static File[] setUpFolder(File folder) {
        assertTrue("cannot find resources in " + folder.getPath(), folder.exists());
        Collection<File> files = FileUtils.listFiles(folder, "xml".split(" "), true);
        files.removeIf(file -> file.getName().equals("doc.xml"));
        File[] res = files.toArray(new File[0]);
        Arrays.sort(res);
        return res;
    }

    @After
    public void tearDown() {
    }

    private class TestCase {

        private String line;
        private String keyword;
        private int countSeparated;
        private int countIncl;

        public TestCase(String line, String keyword, int countSeparated, int countIncl) {
            this.line = line;
            this.keyword = keyword;
            this.countSeparated = countSeparated;
            this.countIncl = countIncl;
        }

    }

    List<TestCase> cases = new LinkedList<>();

    /**
     * Test of countKeyword method, of class KeywordExtractor.
     */
    @Test
    public void testGetKeywordPosition() {
        System.out.println("getKeywordPosition");
        KeywordExtractor instanceTrue = new KeywordExtractor();
        KeywordExtractor instanceFalse = new KeywordExtractor(new QueryConfig.Builder(false, true).build());
        for (TestCase aCase : cases) {
            double[][] keywordPosition = instanceTrue.getKeywordPositions(aCase.keyword, aCase.line);
            for (double[] ds : keywordPosition) {
                Assert.assertEquals(aCase.keyword.length(), (ds[1] - ds[0]) * aCase.line.length(), 1e-4);
            }
            double[][] keywordPosition2 = instanceFalse.getKeywordPositions(aCase.keyword, aCase.line);
            for (double[] ds : keywordPosition2) {
                Assert.assertEquals(aCase.keyword.length(), (ds[1] - ds[0]) * aCase.line.length(), 1e-4);
            }

        }
    }

    private static KWS.Result GT2Hyp(KWS.GroundTruth gt) {
        HashMap<String, KWS.Word> map = new HashMap<>();
        for (KWS.Page page : gt.getPages()) {
            String pageID = page.getPageID();
            List<KWS.Line> lines = page.getLines();
            for (KWS.Line line : lines) {
                HashMap<String, List<Polygon>> keywords = line.getKeyword2Baseline();
                for (String kw : keywords.keySet()) {
                    List<Polygon> positions = keywords.get(kw);
                    if (positions == null || positions.isEmpty()) {
                        continue;
                    }
                    KWS.Word kwsWord = map.get(kw);
                    if (kwsWord == null) {
                        kwsWord = new KWS.Word(kw);
                        map.put(kw, kwsWord);
                    }
                    for (KWS.Entry po : kwsWord.getPos()) {
                        po.setParentLine(line);
                    }
                    for (Polygon position : positions) {
                        kwsWord.add(new KWS.Entry(1.0, line.getLineID(), pageID, position, null));
                    }
                }
            }
        }
        return new KWS.Result(new HashSet<>(map.values()));
    }

    private String[] getStringList(File[] files) {
        String[] res = new String[files.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = files[i].getPath();
        }
        return res;
    }

    @Test
    public void testGenerateKWSGroundTruth() {
        System.out.println("generateKWSGroundTruth");
        KeywordExtractor kwe = new KeywordExtractor();
        List<String> keywords = Arrays.asList("der", "und");
        String[] idListGT = getStringList(listGT);
        String[] idListBot = getStringList(listBot);
        KeywordExtractor.PageIterator pi = new KeywordExtractor.FileListPageIterator(idListGT);
        KeywordExtractor.PageIterator pibot = new KeywordExtractor.FileListPageIterator(idListBot);
        KWS.GroundTruth keywordGroundTruth = kwe.getKeywordGroundTruth(pi, keywords);
        KWS.Result keyWordErr = GT2Hyp(kwe.getKeywordGroundTruth(pibot, keywords));
        LinkedList<IRankingMeasure.Measure> measures = new LinkedList<>();
        measures.add(IRankingMeasure.Measure.GAP);
        measures.add(IRankingMeasure.Measure.MAP);
        Map<IRankingMeasure.Measure, Double> measure = KWSEvaluationMeasure.getMeasure(keyWordErr, keywordGroundTruth, KeyWordMatchers.nearBaselines(), measures);
        System.out.println(measure.get(IRankingMeasure.Measure.GAP));
        System.out.println(measure.get(IRankingMeasure.Measure.MAP));
    }

    public static void main(String[] args) {
        KeywordExtractorTest.setUpClass();
        KeywordExtractorTest.setUpFolder();
        KeywordExtractorTest kwe = new KeywordExtractorTest();
        kwe.setUp();
        kwe.testGenerateKWSGroundTruth();
    }

}
