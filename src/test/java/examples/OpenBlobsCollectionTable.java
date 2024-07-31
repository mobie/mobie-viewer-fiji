package examples;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenCollectionTableCommand;

import java.io.File;

public class OpenBlobsCollectionTable
{
    public static void main( String[] args )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.table = new File( "src/test/resources/collections/blobs-table.txt" );
        command.dataRoot = OpenCollectionTableCommand.DataRoot.UseBelowDataRootFolder;
        command.dataRootFile = new File( "/Users/tischer/Documents/mobie-viewer-fiji/src/test/resources/collections" );
        command.run();
    }
}
