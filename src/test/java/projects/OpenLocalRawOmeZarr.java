package projects;

import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.MoBIESettings;
import de.embl.cba.mobie.source.ImageDataFormat;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenLocalRawOmeZarr {
    public static void main( String[] args ) throws IOException
    {
        final ImageJ imageJ = new ImageJ();
        final MoBIE moBIE = new MoBIE("/home/katerina/Documents/data/data/", MoBIESettings.settings().imageDataFormat( ImageDataFormat.OmeZarr));
    }
}
