package examples;

import net.imagej.ImageJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;
import org.embl.mobie.command.open.OpenCollectionTableCommand;

import java.io.File;

public class OpenCollectionTable
{
    public static void main( String[] args )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        String tablePath = "src/test/resources/collections/blobs-table.txt";
        File tableFile = new File( tablePath );

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.table = tableFile;
        command.run();
    }
}
