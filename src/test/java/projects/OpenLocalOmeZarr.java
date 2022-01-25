package projects;

import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenLocalOmeZarr {
    public static void main(String[] args) throws IOException {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();
        new MoBIE( "/home/katerina/Documents/embl/mnt2/kreshuk/pape/Work/mobie/covid-if-project/data", MoBIESettings.settings().imageDataFormat( ImageDataFormat.OmeZarr ) );
    }
}
