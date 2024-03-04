package projects.colony_detection_anavo;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenHCSDatasetCommand;

import java.io.File;

public class OpenAnavoHCSDataWithVoxelDimensions
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();
        final OpenHCSDatasetCommand command = new OpenHCSDatasetCommand();
        command.hcsDirectory = "/Users/tischer/Desktop/moritz/U2OS_subset";
        command.voxelDimensionFetching = OpenHCSDatasetCommand.VoxelDimensionFetching.FromOMEXML;
        command.omeXML = new File("/Users/tischer/Desktop/moritz/MeasurementResult.ome.xml");
        command.run();
    }
}
