package projects;

import net.imagej.ImageJ;
import org.embl.mobie.io.n5.util.ImageDataFormat;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;

import java.io.IOException;

public class OpenRemoteOmeZarr
{
    public static void main(String[] args) throws IOException
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();
        new MoBIE("https://s3.embl.de/i2k-2020/project-ome-zarr", MoBIESettings.settings().gitProjectBranch( "master" ).imageDataFormat( ImageDataFormat.OmeZarrS3));
    }
}

//https://s3.embl.de/i2k-2020/project-ome-zarr