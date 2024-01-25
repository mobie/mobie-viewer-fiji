package projects.colony_detection_anova;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenTableCommand;

import java.io.File;

public class OpenColonyWellTable
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();
        final OpenTableCommand command = new OpenTableCommand();
        command.root = new File( "/Users/tischer/Desktop/moritz/CQ1_testfiles-wells" );
        command.table = new File( "/Users/tischer/Desktop/moritz/CQ1_testfiles-wells/well_table.csv" );
        command.images = "file_name";
        //command.labels = "Nuclei_Labels_Path=Nuclei,Nuclei_Periphery_Labels_Path=Periphery";
        command.removeSpatialCalibration = true;
        command.run();
    }
}
