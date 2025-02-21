package examples;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenCollectionTableCommand;

import java.io.File;

public class OpenCollectionTable
{
    public static void main( String[] args )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.table = new File( "src/test/resources/collections/organ_spots_collection.txt" );
        command.dataRoot = OpenCollectionTableCommand.DataRoot.UseTableFolder;
        command.dataRootFile = new File( "/Users/tischer/Documents/mobie-viewer-fiji/src/test/resources/collections" );
        command.run();
    }
}
