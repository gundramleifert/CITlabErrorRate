package de.uros.citlab.errorrate.interfaces;


import java.awt.*;

public interface ILine {
    /**
     * transcription of text line. Text should not be null, empty or starting/ending with spaces
     *
     * @return
     */
    String getText();

    /**
     * baseline which goes from left to right under the main body of the text (descenders can to deeper).
     * If no baseline is available, return null
     *
     * @return
     */
    Polygon getBaseline();

    /**
     * polygon which returns the surrounding polyon (or bounding box) of a line
     *
     * @return
     */
    default Polygon getPolygon() {
        return null;
    }

    /**
     * ID which is unique within a page
     *
     * @return
     */
    String getId();
}
