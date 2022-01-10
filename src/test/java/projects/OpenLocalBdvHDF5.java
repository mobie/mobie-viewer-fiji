package projects;

import net.imagej.ImageJ;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;
import org.embl.mobie.viewer.source.ImageDataFormat;

import java.io.IOException;

public class OpenLocalBdvHDF5
{
    public static void main( String[] args ) throws IOException
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();
        final MoBIE moBIE = new MoBIE("/home/katerina/Documents/embl/mnt/kreshuk/kreshuk/pape/Work/mobie/hdf5-example-project", MoBIESettings.settings().imageDataFormat( ImageDataFormat.BdvHDF5));
    }
}