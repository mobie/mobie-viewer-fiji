package projects;

import net.imagej.ImageJ;
import org.embl.mobie.io.n5.util.ImageDataFormat;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;

import java.io.IOException;

public class OpenLocalOmeZarr {
    public static void main(String[] args) throws IOException {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();
        new MoBIE( "/home/katerina/Documents/embl/mnt/kreshuk/pape/Work/mobie/covid-if-project/data", MoBIESettings.settings().imageDataFormat( ImageDataFormat.OmeZarr ) );
    }
}
