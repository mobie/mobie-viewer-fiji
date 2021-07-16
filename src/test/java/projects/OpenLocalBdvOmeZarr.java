package projects;

import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.MoBIESettings;
import de.embl.cba.mobie.source.ImageDataFormat;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenLocalBdvOmeZarr {
    public static void main( String[] args ) throws IOException
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();
        final MoBIE moBIE = new MoBIE("/g/kreshuk/pape/Work/mobie/covid-em-datasets/ngff-example/data", MoBIESettings.settings().imageDataFormat( ImageDataFormat.BdvOmeZarr));
    }
}
