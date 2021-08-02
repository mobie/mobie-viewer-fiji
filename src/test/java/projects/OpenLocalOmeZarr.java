package projects;

import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.MoBIESettings;
import de.embl.cba.mobie.source.ImageDataFormat;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenLocalOmeZarr {
    public static void main(String[] args) throws IOException {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();
        new MoBIE( "/home/katerina/Documents/embl/mnt/kreshuk/pape/Work/mobie/covid-if-project/test-project/data", MoBIESettings.settings().imageDataFormat( ImageDataFormat.OmeZarr ) );
    }
}
