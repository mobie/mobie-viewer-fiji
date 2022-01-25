package projects;


import net.imagej.ImageJ;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;

import java.io.IOException;

public class OpenRemoteSpongeFibsemProject
{
    public static void main(String[] args) throws IOException
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();
//        /g/kreshuk/pape/mobie/covid-if-project
        new MoBIE("https://github.com/mobie/sponge-fibsem-project", MoBIESettings.settings().gitProjectBranch( "spec-v2" ).imageDataFormat( ImageDataFormat.BdvN5S3));
    }
}