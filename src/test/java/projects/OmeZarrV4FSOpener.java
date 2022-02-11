package projects;

import net.imagej.ImageJ;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;

import java.io.IOException;

public class OmeZarrV4FSOpener {
    public static void main(String[] args) throws IOException {
        showYX();
    }

    public static void showYX() throws IOException {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();
        new MoBIE("/home/katerina/Documents/embl/mnt/kreshuk2/kreshuk/pape/Work/mobie/ngff/ome-ngff-prototypes/single_image/v0.4/yx.ome.zarr", MoBIESettings.settings().imageDataFormat( ImageDataFormat.OmeZarr));
    }
}
