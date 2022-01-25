package projects;

import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenLocalBdvOmeZarr {
    public static void main( String[] args ) throws IOException
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();
        final MoBIE moBIE = new MoBIE("/home/katerina/Documents/embl/mnt3/kreshuk/pape/Work/mobie/covid-if-project", MoBIESettings.settings().imageDataFormat( ImageDataFormat.OmeZarr));
    }
}
