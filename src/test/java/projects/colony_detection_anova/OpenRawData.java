package projects.colony_detection_anova;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenHCSDatasetCommand;
import org.embl.mobie.command.open.OpenTableCommand;

import java.io.File;

public class OpenRawData
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();
        final OpenHCSDatasetCommand command = new OpenHCSDatasetCommand();
        command.hcsDirectory = "/Users/tischer/Desktop/moritz/U2OS_subset";
        command.run();
    }
}
