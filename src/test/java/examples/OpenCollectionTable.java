package examples;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenCollectionTableCommand;

public class OpenCollectionTable
{
    public static void main( String[] args )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        //command.table = new File( "src/test/resources/collections/organ_spots_collection.tsv" );
        //command.table = new File( "src/test/resources/collections/blobs-grid-table.txt" );
        //command.table = new File( "src/test/resources/collections/blobs-grid-table-grid-pos.txt" );
        //command.table = new File( "/Users/tischer/Documents/bacteria-fluorescent-foci-analysis/data/local/collection-grid.txt" );
        command.tableUri = "src/test/resources/collections/blobs-mixed-datatypes.txt";
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseTableFolder;
        command.run();
    }
}
