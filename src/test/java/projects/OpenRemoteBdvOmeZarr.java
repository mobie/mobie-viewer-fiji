package projects;

import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.MoBIESettings;
import de.embl.cba.mobie.source.ImageDataFormat;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenRemoteBdvOmeZarr {
    public static void main(String[] args) throws IOException {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();
        new MoBIE("https://s3.embl.de/i2k-2020/project-bdv-ome-zarr", MoBIESettings.settings().imageDataFormat(ImageDataFormat.BdvOmeZarr));
    }
}