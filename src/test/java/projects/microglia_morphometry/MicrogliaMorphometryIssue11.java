package projects.microglia_morphometry;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenMultipleImagesAndLabelsCommand;
import org.embl.mobie.command.open.OpenTableCommand;

import java.io.File;

public class MicrogliaMorphometryIssue11
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();

        OpenTableCommand command = new OpenTableCommand();
        command.root = new File("/Users/tischer/Desktop/microglia-data/test");
        command.table = new File("/Users/tischer/Desktop/microglia-data/test/test-crop-8bit-ds2.csv");
        command.images = "Path_Intensities=Signal";
        command.labels = "Path_LabelMasks=Segmentation";
        command.removeSpatialCalibration = true;
        command.run();
    }
}
