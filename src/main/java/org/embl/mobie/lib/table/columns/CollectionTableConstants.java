package org.embl.mobie.lib.table.columns;

/**
 * Class that specifies the column names and corresponding
 * row values of a "MoBIE collection table".
 *
 * Tables following this specification can be opened in MoBIE/Fiji via
 * MoBIE..Open..Open Collection Table...
 *
 * The table currently MUST be a TAB separated text file.
 * - see the "affine" column for why we currently cannot support COMMA separation
 *
 * In addition to the columns that are specified in this class
 * tables MAY have as many additional columns as needed.
 */
public class CollectionTableConstants
{
    /**
     * The "uri" (or "File Path") column MUST be present
     * and it MUST point to a valid image, labels or spots dataset.
     *
     * The same URI MAY be present several times in the same table;
     * this can be useful to display the same data within various views,
     * or with different transformations.
     *
     * Supported values:
     * - Local files system paths
     * - AWS S3 URLs
     *
     * Supported image formats include:
     * - OME-Zarr (local and on S3)
     * - Everything that Bio-Formats can open
     * - BDV XML (incl. HDF5 and N5)
     * - ilastik hdf5
     *
     * Support spots file formats include:
     * - Parquet
     * - Tab or comma separated value text files
     * - Google Sheets URLs
     *
     */
    public static final String[] URI = {
            "uri", // MoBIE
            "File Path" // BioFile Finder
    };

    /**
     * The "name" column MAY be present.
     *
     * This "name" will determined how the data is called within MoBIE.
     * In the MoBIE UI this name will show up as a label where the
     * display settings can be assigned.
     *
     * This is useful for assigning data a nice and meaningful names.
     *
     * This is important if two image data sets have the same file name,
     * because in this case they would "overwrite" each other in MoBIE
     * and only the last one in the table would be accessible.
     *
     * Supported values:
     * - Free text
     *
     * Default value:
     * - If the column is absent or the cell is empty a name will be automatically assigned
     *   from the file name part of the URI
     */
    public static final String NAME = "name";

    /**
     * The "type" column MAY be present.
     *
     * It specifies what pixel type the data is.
     * This determines how the data is rendered and can be interacted with.
     *
     * Supported values:
     * - "intensities"
     * - "labels"
     * - "spots"
     *
     * Default value: "intensities"
     * The default value will be assigned if
     * - the column is absent.
     * - the value is none of the supported values.
     */
    public static final String TYPE = "type";
    public static final String INTENSITIES = "intensities";
    public static final String LABELS = "labels";
    public static final String SPOTS = "spots";


    /**
     * The "channel" column MAY be present.
     *
     * The value determines which channel of a multi-channel dataset will be loaded.
     * If one wishes to load several channels of the same dataset, one MUST add
     * correspondingly many table rows with the *same URI* and the respective
     * different value in the "channel" column.
     *
     * Discussion points:
     * - One could consider to encode the channel directly within the URI
     *   - see: https://forum.image.sc/t/loading-only-one-channel-from-an-ome-zarr/97798
     *
     * Supported values:
     * - Zero based positive integers
     *
     * Default value: 0
     * The default value will be assigned if
     * - this column is absent.
     * - the value cannot be parsed to a positive integer.
     */
    public static final String CHANNEL = "channel";

    /**
     * The "color" column MAY be present.
     *
     * The value determines the lookup table coloring for this image.
     * It only applies for "intensities", it is ignored for "labels".
     *
     * Supported values include:
     * - e.g., "r(0)-g(255)-b(0)-a(255)"
     * - e.g., "white", "red", ....
     *
     * Default: "white"
     * If the column is absent or the value cannot be parsed,
     * the color will be "white".
     *
     */
    public static final String COLOR = "color";

    /**
     * The "blend" column MAY be present.
     *
     * The value determines the blending mode for this image.
     * For the "alpha" blending mode the sequence of the images
     * in the table matters. Images that come later in the table are 
     * "on top" of earlier images.
     *
     * Supported values:
     * - "sum"
     * - "alpha"
     *
     * Default: "sum"
     * If the column is absent or the value is not supported,
     * the blending mode will be "sum".
     *
     */
    public static final String BLEND = "blend";


    /**
     * The "affine" column MAY be present.
     *
     * The value will determine an affine transformation that will
     * be applied to the image upon display, i.e. it will change where
     * the image will be rendered in the viewer.
     *
     * Supported values:
     * - bracketed, comma separated, row packed floating point values
     * - e.g., identity transform: (1,0,0,0,0,1,0,0,0,0,1,0)
     * - e.g., shift along x-axis: (1,0,0,-105.34,0,1,0,0,0,0,1,0)
     *
     * Default: No transformation
     * If the column is absent or the value cannot be parsed no
     * additional transformation will be applied on top of the
     * transformation that is found within the image data itself.
     *
     * Notes:
     * - This affine transformation will be applied on top of any transformation
     *   that can be discovered within the image URI
     */
    public static final String AFFINE = "affine";

    /**
     * The "tps" column MAY be present.
     *
     * The value defines a thin plate spline (tps) transformation that will
     * be applied to the image upon display, i.e. it will change where
     * the image will be rendered in the viewer.
     *
     * Supported values:
     * - BigWarp Landmark JSON
     *
     * Default: No transformation
     * If the column is absent or the value cannot be parsed, no
     * additional transformation will be applied on top of the
     * transformation that is found within the image data itself.
     *
     * Notes:
     * - This tps transformation will be applied on top of any transformation
     *   that can be discovered within the image URI
     * - If you also specified an affine transformation for this image, the thin plate spline
     *   transformation will be applied after the affine transformation
     */
    public static final String TPS = "tps";

    /**
     * The "view" column MAY be present.
     *
     * The value will determine to which view this image will be added,
     * i.e. at which name it can be accessed in the MoBIE UI.
     *
     * Supported values:
     * - Free text
     *
     * Default: value determined from NAME column
     * If the column is absent or if the value is an empty string,
     * the name of the view will be set equal to value determined
     * by the NAME column (see above).
     *
     * Use cases:
     * - One can combine several images into the same view, e.g.
     *   different channels of the same image, or an image and a corresponding
     *   label mask (segmentation) image, or several (registered) images of
     *   a CLEM experiment.
     */
    public static final String VIEW = "view";

    /**
     * The "exclusive" column MAY be present.
     *
     * The value will determine whether the view that is associated to
     * this row will be exclusive.
     * If exclusive = true, upon showing the view all currently displayed
     * items will be removed.
     * If exclusive = false, the view will be display in addition to all
     * currently displayed items.
     *
     * Supported values:
     * - "true"
     * - "false"
     *
     * Default: "false"
     * If the column is absent or the String is none of the supported values
     * the view will not be exclusive.
     *
     * Use cases:
     * - If there is data in the table that is displayed in different coordinate systems
     *   it can make sense to avoid that they will be shown together.
     */
    public static final String EXCLUSIVE = "exclusive";
    public static final String TRUE = "true";
    public static final String FALSE = "false"; // default


    /**
     * The "group" column MAY be present.
     *
     * The value will create a UI selection group(s) in the MoBIE user interface
     * to which the view of this image will be added.
     *
     * Supported values:
     * - Free text (without ","), e.g.: "em"
     * - Comma separated list of groups, e.g.: "nice,great"
     *   - The same view will be selectable from several group drop-downs
     *
     * Default: "views"
     * If the column is absent or contains an empty string the
     * view that corresponds to this row will be put in
     * a UI selection group called "views".
     *
     * Use cases:
     * - If you have a lot of data it can be helpful to
     *   divide the views into groups.
     */
    public static final String GROUP = "group";

    /**
     * The "labels_table" column MAY be present.
     *
     * The value is ONLY used when the "type" column has the value "labels",
     * otherwise it is ignored.
     *
     * Supported values:
     * - A valid uri to a "segmentation" table.
     * - For supported columns in segmentation tables see, e.g.
     *   {@code MoBIESegmentColumnNames} or {@code SkimageSegmentColumnNames}.
     *
     * Default:
     * If the column is absent or contains an empty string
     * no labels table will be loaded.
     *
     * Use cases:
     * - Exploration of measurements corresponding to the labels
     */
    public static final String LABELS_TABLE = "labels_table";
    public static final String LABELS_TABLE_URI = "labels_table_uri";

    /**
     * The "contrast_limits" column MAY be present.
     *
     * The value is ONLY used when the "type" column has the value "intensities",
     * or if the "type" column is absent, which causes the type to default to "intensities",
     * otherwise this value is ignored.
     * Supported values:
     * - Bracketed, comma (or semi-colon) separated list of min and max, e.g.
     *   - (10,240)
     *
     * Default:
     * If the column is absent or can't be parsed, MoBIE will apply an
     * auto-contrast algorithm, if the data is not too big. // TODO explain more
     * If the data is too big, the contrast limits will be set to
     * the limits of the pixel data type.
     *
     * Use cases:
     * - Adjust the contrast limits such that the intensities are readily visible
     *
     */
    public static final String CONTRAST_LIMITS = "contrast_limits";

    /**
     * The "grid" column MAY be present.
     *
     * All images or segmentations that share the same "grid" will be shown together
     * as a grid view.
     *
     * All images or segmentations in a grid view will be displayed with the same
     * display settings, thus they SHOULD have the same "contrast_limits" and "color".
     * If those entries are not the same, MoBIE will randomly apply one of them.
     *
     * Images or segmentations within the same "grid" MUST be part of the same "view"
     *
     * Supported values:
     * - Free text
     *
     * Default: No grid
     * If the column is absent or contains an empty string the
     * data in this row will not be part of a grid.
     *
     * Use cases:
     * - Display similar data together such that it can be readily compared
     * - Plate/well based high-throughput microscopy data is a typical use-case
     */
    public static final String GRID = "grid";


    /**
     * The "grid_position" column MAY be present.
     *
     * FIXME
     * The value is ONLY used when the "type" column has the value "intensities",
     * or if the "type" column is absent, which causes the type to default to "intensities",
     * otherwise this value is ignored.
     * Supported values:
     * - Bracketed or quoted, comma or semi-colon separated list of min and max, e.g.
     *   - (10;240)
     *   - "10,240"
     *
     * Default:
     * If the column is absent or can't be parsed, MoBIE will apply an
     * auto-contrast algorithm, if the data is not too big. // TODO explain more
     * If the data is too big, the contrast limits will be set to
     * the limits of the pixel data type.
     *
     * Use cases:
     * - Adjust the contrast limits such that the intensities are readily visible
     *
     */
    public static final String GRID_POSITION = "grid_position";

    /**
     * The "display" column MAY be present.
     *
     * Images or segmentations within the same "display" MUST be part of the same "view"
     *
     * All images or segmentations that share the same "display"  will be displayed with the same
     * display settings.
     *
     *
     * Supported values:
     * - Free text
     *
     * Default:
     * If the column is absent or contains an empty string and there is no "grid" column
     * a display will be created just for this dataset.
     * If there is a "grid" column the "grid" will be used as the "display".
     *
     * Use cases:
     * - A "display" is needed when there is a "grid"
     *   in which data with different display settings should be overlaid
     * - A "display" is useful, too, if multiple data are stitched via an "affine" transformation
     */
    public static final String DISPLAY = "display";

    /**
     * The "format" column MAY be present.
     *
     * Supported values:
     * - OmeZarr
     *
     * Default:
     * If the column is absent or contains an empty or unsupported string the
     * data format will be determined from the file ending
     *
     * Use cases:
     * - OME-Zarr data that does not contain ".ome.zarr" in the path
     */
    public static final String FORMAT = "format";

    /**
     * The "spot_radius" column MAY be present.
     *
     * Supported values:
     * - Numeric
     *
     * Default:
     * If the column is absent or contains an empty or unsupported string the
     * spot radius will be set to 1.0
     *
     * Use cases:
     * - Configure how large spots will appear
     */
    public static final String SPOT_RADIUS = "spot_radius";

    /**
     * The "bounding_box" column MAY be present.
     *
     * Supported values:
     * - two (min and max) bracket comma separated tuple of 3 (x,y,z) floating point values separated with a "-"
     * - Example: (0.0,0,0)-(200.5,200,50.3)
     *
     * Default:
     * If the column is absent or contains an empty or unsupported string the
     * bounding box will not be set but automatically computed from the data source
     *
     * Use cases:
     * - Configure the extent of a spot image
     */
    public static final String BOUNDING_BOX = "bounding_box";
}
