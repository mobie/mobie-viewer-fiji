package projects;

import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.MoBIESettings;
import de.embl.cba.mobie.source.ImageDataFormat;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenOpenOrganell
{
    public static void main( String[] args )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();
        try {
            new MoBIE("/home/katerina/Documents/data/organel/open-organelle-test-master",
                    MoBIESettings.settings().imageDataFormat( ImageDataFormat.OpenOrganelleS3) );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
