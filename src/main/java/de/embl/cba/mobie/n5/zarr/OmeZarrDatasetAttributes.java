package de.embl.cba.mobie.n5.zarr;

import org.janelia.saalfeldlab.n5.Compression;

public class OmeZarrDatasetAttributes extends ZarrDatasetAttributes {
    private ZarrAxes axes;

    public OmeZarrDatasetAttributes( long[] dimensions, int[] blockSize, DType dType, Compression compression,
                                     boolean isRowMajor, String fill_value, ZarrAxes axes ) {
        super(dimensions, blockSize, dType, compression, isRowMajor, fill_value);
        this.axes = axes;
    }

    public ZarrAxes getAxes() {
        return axes;
    }
}
