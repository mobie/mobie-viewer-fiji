package projects.colony_detection_anavo;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenHCSDatasetCommand;

public class OpenRawData
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();
        final OpenHCSDatasetCommand command = new OpenHCSDatasetCommand();
        command.hcsDirectory = "/Users/tischer/Desktop/moritz/U2OS_subset";
        command.hcsDirectory = "/Users/tischer/Desktop/moritz/CQ1_testfiles";
        command.run();
    }
}
