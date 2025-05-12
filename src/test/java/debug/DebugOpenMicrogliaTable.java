package debug;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.special.OpenMicrogliaTableCommand;
import org.embl.mobie.lib.table.saw.TableOpener;
import tech.tablesaw.api.Table;

import java.io.File;

public class DebugOpenMicrogliaTable
{
    public static void main( String[] args )
    {
        Table table = TableOpener.openDelimitedFile( "/Users/tischer/Desktop/valerie/Image_892C-1.csv" );

        new ImageJ().ui().showUI();
//        OpenTableCommand command = new OpenTableCommand();
//        command.table = new File("/Users/tischer/Desktop/valerie/Human_microglia_morphometry/Image_892C-1.csv");
//        command.root = new File("/Users/tischer/Desktop/valerie/Human_microglia_morphometry");
//        command.images = "Path_Intensities";
//        command.labels = "Path_LabelMasks";
//        command.pathMapping = "";
//        command.run();

        OpenMicrogliaTableCommand command = new OpenMicrogliaTableCommand();
        command.table = new File("/Users/tischer/Desktop/valerie/Human_microglia_morphometry/Image_892C-1.csv");
        command.run();
        //
    }
}
