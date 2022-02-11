package projects;

import net.imagej.ImageJ;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;

import java.io.IOException;

public class OmeZarrS3V4Opener {
    public static void main(String[] args) throws IOException {
        showYX();
    }

    public static void showYX() throws IOException {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();
        final MoBIE moBIE = new MoBIE("https://s3.embl.de/i2k-2020/ngff-example-data/v0.4/yx.ome.zarr", MoBIESettings.settings().imageDataFormat( ImageDataFormat.OmeZarrS3));
    }
}
