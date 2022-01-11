package projects;

import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;
import org.embl.mobie.viewer.source.ImageDataFormat;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenLocalBdvOmeZarr {
    public static void main( String[] args ) throws IOException
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();
        final MoBIE moBIE = new MoBIE("/home/katerina/Documents/embl/mnt/kreshuk3/kreshuk/pape/Work/mobie/covid-em-datasets/ngff-example/data", MoBIESettings.settings().imageDataFormat( ImageDataFormat.OmeZarr));
    }
}
