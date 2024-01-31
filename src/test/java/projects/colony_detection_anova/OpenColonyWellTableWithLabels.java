package projects.colony_detection_anova;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenTableCommand;

import java.io.File;

public class OpenColonyWellTableWithLabels
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();
        final OpenTableCommand command = new OpenTableCommand();
        //command.root = new File( "/Users/tischer/Desktop/moritz/CQ1_testfiles-wells" );
        //command.table = new File( "/Users/tischer/Desktop/moritz/CQ1_testfiles-wells/well_table_with_colonies.csv" );
        command.root = new File( "/Users/tischer/Desktop/moritz/U2OS_subset-wells" );
        command.table = new File( "/Users/tischer/Desktop/moritz/U2OS_subset-wells/well_table_with_colonies.csv" );
        command.images = "file_name";
        command.labels = "labels_file_name";
        command.removeSpatialCalibration = true;
        command.run();
    }
}
