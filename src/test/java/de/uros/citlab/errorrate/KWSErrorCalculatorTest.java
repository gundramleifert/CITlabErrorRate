/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate;

import de.uros.citlab.errorrate.kws.measures.IRankingMeasure;
import org.apache.commons.io.FileUtils;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author gundram
 */
public class KWSErrorCalculatorTest {

    public static final File folderTmp = new File("src/test/resources/test_kws_tmp");
    private static final File folderGT = new File("src/test/resources/gt");

    public KWSErrorCalculatorTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        folderTmp.mkdirs();
    }

    @AfterClass
    public static void tearDownClass() {
        FileUtils.deleteQuietly(folderTmp);
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of main method, of class KwsError.
     */
    @Test
    public void testRun() throws IOException {
        System.out.println("testRun");
        Collection<File> listFiles = FileUtils.listFiles(folderGT, "xml".split(" "), true);
        List<File> tmp = new LinkedList<File>(listFiles);
        Collections.sort(tmp);
        listFiles = tmp;
        File gtList = new File("src/test/resources/gt.json");
        File resFile = new File("src/test/resources/kws_htr/out_20.json");
        KwsError calculator = new KwsError();
        TreeMap<IRankingMeasure.Measure, Double> exp = new TreeMap<>();
        exp.put(IRankingMeasure.Measure.R_PRECISION, 0.825503355704698);
        exp.put(IRankingMeasure.Measure.G_NCDG, -0.018474595791665722);
        exp.put(IRankingMeasure.Measure.MAP, 0.9002287096132139);
        exp.put(IRankingMeasure.Measure.GAP, 0.8987380224491166);
        exp.put(IRankingMeasure.Measure.RECALL, 0.9765100671140939);
        exp.put(IRankingMeasure.Measure.PRECISION_AT_10, 0.5054545454545453);
        exp.put(IRankingMeasure.Measure.PRECISION, 0.41512125534950073);
        exp.put(IRankingMeasure.Measure.M_NCDG, 0.3827312294124316);
        exp.put(IRankingMeasure.Measure.WMAP,0.9141140577336244);
        exp.put(IRankingMeasure.Measure.AVERAGE_MATCHER_CONFIDENCE,0.9388115297609249);
        Map<IRankingMeasure.Measure, Double> run = calculator.run(new String[]{resFile.getPath(), gtList.getPath()});
        for (IRankingMeasure.Measure measure : run.keySet()) {
//            System.out.println(measure + " = " + run.get(measure));
            Assert.assertEquals("measure changed (" + measure + ")", exp.get(measure), run.get(measure), 0.000001);
        }
        // TODO review the generated test code and remove the default call to fail.
    }

    public static void min(String[] args) throws IOException {
        KWSErrorCalculatorTest.setUpClass();
        KWSErrorCalculatorTest t = new KWSErrorCalculatorTest();
        t.testRun();
        KWSErrorCalculatorTest.tearDownClass();
    }

}
