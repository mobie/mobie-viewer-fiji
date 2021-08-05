package de.embl.cba.mobie.n5.zarr;

import org.janelia.saalfeldlab.n5.N5Reader;

public class OmeZarrMultiscales {
    public ZarrAxes axes;
    public Dataset[] datasets;
    public String name;
    public String type;
    public N5Reader.Version version;

    public OmeZarrMultiscales(){}

    public static class Dataset {
        public String path;
    }
}
