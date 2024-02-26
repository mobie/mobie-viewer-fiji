package projects.colony_detection_anavo;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenTableCommand;

import java.io.File;

public class OpenColonyObjectsTable
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();
        final OpenTableCommand command = new OpenTableCommand();
        command.root = new File( "/Users/tischer/Desktop/moritz/HCT116_dataset-wells" );
        command.table = new File( "/Users/tischer/Desktop/moritz/HCT116_dataset-wells/colony_table.csv" );
        command.images = "file_name";
        command.labels = "labels_file_name";
        command.removeSpatialCalibration = true;
        command.run();
    }
}
