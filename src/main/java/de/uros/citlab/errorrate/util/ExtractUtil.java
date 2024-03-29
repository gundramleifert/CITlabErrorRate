package de.uros.citlab.errorrate.util;

import de.uros.citlab.errorrate.interfaces.ILine;
import org.primaresearch.dla.page.Page;
import org.primaresearch.dla.page.layout.physical.Region;
import org.primaresearch.dla.page.layout.physical.text.LowLevelTextObject;
import org.primaresearch.dla.page.layout.physical.text.impl.TextLine;
import org.primaresearch.dla.page.layout.physical.text.impl.TextRegion;
import org.primaresearch.io.UnsupportedFormatVersionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ExtractUtil {
    private static Logger LOG = LoggerFactory.getLogger(ExtractUtil.class);

    private static Polygon getPolygon(org.primaresearch.maths.geometry.Polygon baseline) {
        if (baseline == null) {
            return null;
        }
        Polygon p = new Polygon();
        for (int i = 0; i < baseline.getSize(); i++) {
            p.addPoint(baseline.getPoint(i).x, baseline.getPoint(i).y);
        }
        return p;
    }

    public static List<String> getTextFromFile(File fileName) {
        return getTextFromFile(fileName.getPath());
    }

    public static List<String> getTextFromFile(String fileName) {
        if (fileName.endsWith(".xml")) {
            Page aPage;
            try {
                List<String> res = new ArrayList<>();
//                aPage = reader.read(new FileInput(new File(fileName)));
                aPage = org.primaresearch.dla.page.io.xml.PageXmlInputOutput.readPage(fileName);
                if (aPage == null) {
                    System.out.println("Error while parsing xml-File.");
                    return null;
                }
                List<Region> regionsSorted = aPage.getLayout().getRegionsSorted();
                for (Region reg : regionsSorted)
                    if (reg instanceof TextRegion) {
                        TextRegion textregion = (TextRegion) reg;
                        List<LowLevelTextObject> textObjectsSorted = textregion.getTextObjectsSorted();
                        for (LowLevelTextObject line : textObjectsSorted) {
                            if (line instanceof TextLine) {
                                String text = line.getText();
                                if (text == null) {
                                    LOG.warn("transciption is null for line with id = {} - ignore line.", line.getId());
                                    continue;
                                }
                                if (text.startsWith(" ")) {
                                    LOG.warn("transciption of line id {} starts with space, trim transcription '{}'", line.getId(), text);
                                }
                                if (text.endsWith(" ")) {
                                    LOG.warn("transciption of line id {} ends with space, trim transcription '{}'", line.getId(), text);
                                }
                                res.add(text.trim());
                            }
                        }
                    }
                return res;
            } catch (UnsupportedFormatVersionException ex) {
                throw new RuntimeException("Error while parsing xml-file.", ex);
            }
        }
        return null;
    }

    public static List<ILine> getLinesFromFile(File fileName) {
        return getLinesFromFile(fileName.getPath());
    }

    public static List<ILine> getLinesFromFile(String fileName) {
        if (fileName.endsWith(".xml")) {
            Page aPage;
            try {
                List<ILine> res = new ArrayList<>();
//                aPage = reader.read(new FileInput(new File(fileName)));
                aPage = org.primaresearch.dla.page.io.xml.PageXmlInputOutput.readPage(fileName);
                if (aPage == null) {
                    System.out.println("Error while parsing xml-File.");
                    return null;
                }
                List<Region> regionsSorted = aPage.getLayout().getRegionsSorted();
                for (Region reg : regionsSorted)
                    if (reg instanceof TextRegion) {
                        TextRegion textregion = (TextRegion) reg;
                        List<LowLevelTextObject> textObjectsSorted = textregion.getTextObjectsSorted();
                        for (LowLevelTextObject line : textObjectsSorted) {
                            if (line instanceof TextLine) {
                                String text = line.getText();
                                org.primaresearch.maths.geometry.Polygon baseline = ((TextLine) line).getBaseline();
                                Polygon polygon = getPolygon(baseline);
                                Polygon coord = getPolygon(((TextLine) line).getCoords());
                                if (text == null) {
                                    LOG.warn("transciption is null for line id = {} and document {} - ignore line.", line.getId(), fileName);
                                    continue;
                                }
                                if (polygon == null || polygon.npoints < 2) {
                                    LOG.error("polygon {} of line id = {} and document {} is null or has < 2 points", polygon, line.getId(), fileName);
                                    throw new RuntimeException("polygon " + polygon + " of line with id = " + line.getId() + " is null or has < 2 points");
                                }
                                if (text.startsWith(" ")) {
                                    LOG.warn("transciption of line id {} and document {} starts with space, trim transcription '{}'", line.getId(), fileName, text);
                                }
                                if (text.endsWith(" ")) {
                                    LOG.warn("transciption of line id {} and document {} ends with space, trim transcription '{}'", line.getId(), fileName, text);
                                }
                                final String textFinal = text.trim();
                                if (text.isEmpty()) {
                                    LOG.warn("transciption is empty for line id {} and document {} - ignore line", line.getId(), fileName);
                                    continue;
                                }
                                res.add(new ILine() {
                                    @Override
                                    public String getText() {
                                        return textFinal;
                                    }

                                    @Override
                                    public Polygon getBaseline() {
                                        return polygon;
                                    }

                                    @Override
                                    public Polygon getPolygon() {
                                        return coord;
                                    }

                                    @Override
                                    public String getId() {
                                        return line.getId().toString();
                                    }

                                });
                            }
                        }
                    }
                return res;
            } catch (UnsupportedFormatVersionException ex) {
                throw new RuntimeException("Error while parsing xml-file.", ex);
            }
        }
        return null;
    }

}
