package develop;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenCollectionTableCommand;

import java.io.File;

public class OpenPaoloFirstTable
{
    public static void main( String[] args )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

//        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
//        command.table = new File( "/Users/tischer/Desktop/Paolo.txt" );
//        command.run();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.table = new File( "/Users/tischer/Desktop/Paolo-relative.txt" );
        command.dataRoot = new File( "/Volumes/emcf/ronchi/MRC-MM/aligned" );
        command.run();
    }
}
