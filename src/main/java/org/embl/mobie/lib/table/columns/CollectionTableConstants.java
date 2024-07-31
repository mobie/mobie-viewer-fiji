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
     * The "uri" column MUST be present.
     *
     * The same URI MAY be present several times in the same table;
     * this can be useful to display the same data within various views,
     * or with different transformations.
     *
     * Supported values:
     * - Local files system paths
     * - AWS S3 URLs
     *
     * Supported image file formats include:
     * - OME-Zarr
     * - Everything that Bio-Formats can open
     * - BDV XML (incl. HDF5 and N5)
     * - ilastik hdf5
     */
    public static final String URI = "uri";

    /**
     * The "type" column MAY be present.
     *
     * It specifies what pixel type the data is.
     * This determines how the data is rendered and can be interacted with.
     *
     * Supported values:
     * - "intensities"
     * - "labels"
     *
     * Default value: "intensities"
     * The default value will be assigned if
     * - the column is absent.
     * - the value is none of the supported values.
     */
    public static final String TYPE = "type";
    public static final String INTENSITIES = "intensities";
    public static final String LABELS = "labels";

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
     * Default value: "white"
     * The default value will be assigned if
     * - this column is absent.
     * - the value cannot be parsed to a color by [this code]().
     */
    public static final String COLOR = "color";

    /**
     * The "blend" column MAY be present.
     *
     * The value determines the blending mode for this image.
     *
     * The value MUST be one of "sum" or "alpha".
     *
     * Default value: "sum"
     * The default value will be assigned if
     * - this column is absent.
     * - the value cannot be parsed to a {@code BlendingMode}
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
     * Default value: There is no default value.
     * No transformation will be applied if
     * - this column is absent.
     * - the given value cannot be parsed.
     *
     * Notes:
     * - This affine transformation will be applied on top of any transformation
     *   that can be discovered within the image URI
     */
    public static final String AFFINE = "affine";

    /**
     * The "view" column MAY be present.
     *
     * The value will determine to which view this image will be added,
     * i.e. at which name it can be accessed in the MoBIE UI.
     *
     * Supported values:
     * - Free text
     *
     * Default value: There is no default value.
     * No additional view will be assigned if
     * - this column is absent.
     * - the value is empty.
     *
     * Use cases:
     * - One can add data from the same URI a second time, but
     *   with a different "affine" transform, or a different "channel"
     * - One can combine several images into the same view, e.g.
     *   different channels of the same image, or an image and a corresponding
     *   label mask (segmentation) image, or several (registered) images of
     *   a CLEM experiment.
     */
    public static final String VIEW = "view";

    /**
     * The "group" column MAY be present.
     *
     * The value will create a UI selection group in the MoBIE user interface
     * to which the view of this image will be added.
     *
     * Supported values:
     * - Free text
     *
     * Default value: "views"
     * The default value will be assigned if
     * - this column is absent.
     * - the table cell is empty.
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
     * - A valid path to a "segmentation" table.
     * - For supported columns in segmentation tables see, e.g.
     *   {@code MoBIESegmentColumnNames} or {@code SkimageSegmentColumnNames}.
     *
     * Default value: N/A
     *
     * Use cases:
     * - Exploration of measurements corresponding to the labels
     */
    public static final String LABEL_TABLE = "labels_table";

    /**
     * The "contrast_limits" column MAY be present.
     *
     * The value is ONLY used when the "type" column has the value "intensities",
     * or if the "type" column is absent, which causes the type to default to "intensities",
     * otherwise this value is ignored.
     * Supported values:
     * - Bracketed, comma separated list of min and max,  e.g.
     *   - (10,240)
     *
     * Default value: N/A
     *
     * Use cases:
     * - Adjust the contrast limits such that the intensities are readily visible
     *
     * Notes:
     * - One could consider supporting "auto" here as another supported value
     */
    public static final String CONTRAST_LIMITS = "contrast_limits";
}
