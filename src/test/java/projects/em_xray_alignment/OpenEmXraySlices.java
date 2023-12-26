package projects.em_xray_alignment;

import net.imagej.ImageJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.command.open.OpenMultipleImagesAndLabelsCommand;
import org.embl.mobie.io.OpenerLogging;

import java.io.File;
import java.io.IOException;

public class OpenEmXraySlices
{
    public static void main( String[] args ) throws IOException
    {
       // OpenerLogging.setLogging( true );
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();
        OpenMultipleImagesAndLabelsCommand command = new OpenMultipleImagesAndLabelsCommand();
        command.image0 = new File("/Users/tischer/Desktop/em-xray/xray-slice-ds-0.tif");
        command.image1 = new File("/Users/tischer/Desktop/em-xray/em-slice-ds-0.tif");
        command.run();
    }
}
