package org.embl.mobie.lib.table.columns;

public class CollectionTableConstants
{
    // Column names and vocabulary

    public static final String URI = "uri"; // MUST

    public static final String NAME = "name"; // MAY; free; default: derived from file name of URI

    public static final String PIXEL_TYPE = "type"; // MAY; controlled; default: "intensities"
    public static final String INTENSITIES = "intensities";
    public static final String LABELS = "labels";

    public static final String CHANNEL = "channel"; // MAY; zero-based positive integer, default: 0

    public static final String COLOR = "color"; // MAY; controlled; default: "White"

    public static final String AFFINE = "affine"; // MAY; default: No transform

    public static final String VIEW = "view"; // MAY; default: No extra view, each row will get its own view anyway

}
