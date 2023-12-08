package projects;

import net.imagej.ImageJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.command.open.OpenTableCommand;

import java.io.File;

public class OpenVeronicaMicroglia
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();
        final OpenTableCommand command = new OpenTableCommand();
        command.root = new File( "/Users/tischer/Desktop/veronica" );
        command.table = new File( "/Users/tischer/Desktop/veronica/Intensities c3 pup4 lps.csv" );
        command.images = "Path_Intensities=Signal";
        command.labels = "Path_LabelMasks=Segmentations,Path_Skeletons=Skeletons,Path_Annotations=Annotations";
        command.removeSpatialCalibration = false;
        command.run();
    }
}
