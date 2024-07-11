package org.embl.mobie.lib.table.columns;

/**
 * Class that specifies the column names and corresponding
 * row values of a "MoBIE collection table".
 *
 * Tables following this specification can be opened in MoBIE/Fiji via
 * "MoBIE>Open>Open Collection Table..."
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
     * The "name" column MAY be present.
     *
     * It specifies how the default view for this data is called within the MoBIE UI.
     *
     * Supported values:
     * - Free text (Recommendation: keep it short)
     *
     * Default value: a name will be automatically derived from the URI.
     * The default value will be assigned if
     * - this column is absent.
     * - the value is empty.
     *
     * The name SHOULD be unique;
     * if there are several rows with the same name,
     * only the last one will be accessible in the MoBIE UI.
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
     * The "affine" column MAY be present.
     *
     * The value will determine an affine transformation that will
     * be applied to the image upon display, i.e. it will change where
     * the image will be rendered in the viewer.
     *
     * Supported values:
     * - bracketed, comma separated, row packed floating point values
     * - e.g., identity transform: "(1,0,0,0,0,1,0,0,0,0,1,0)"
     * - e.g., shift along x-axis: "(1,0,0,-105.34,0,1,0,0,0,0,1,0)"
     *
     * Default value: There is no default value.
     * No transformation will be applied if
     * - this column is absent.
     * - the given value cannot be parsed.
     *
     * Notes:
     * - This affine transformation will be applied on top of any transformation
     *   that can be discovered within the image URI
     *
     * Discussion points:
     * - If one would NOT have COMMA to separate the values of the affine
     *   also a CSV would be fine as a table format (current we need TAB)
     *   - For instance, we could use space as a separator instead of comma
     *   - If someone opens the table by chance in Excel, using both TAB and COMMA as a separator
     *     it can lead to a major fuck-up that can initially even go unnoticed
     */
    public static final String AFFINE = "affine";

    /**
     * The "view" column MAY be present.
     *
     * The value will determine to which view this image will be added.
     * Note that each image will be anyway visible via its own view,
     * whose name is determined by the "name" column.
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
     * - One can add data from an URI a second time, but
     *   with a different "affine" transform, or a different "channel"
     * - One can combine several images into the same view, e.g.
     *   different channels of the same image, or an image and a corresponding
     *   label mask (segmentation) image, or several (registered) images of
     *   a CLEM experiment.
     */
    public static final String VIEW = "view";
}
