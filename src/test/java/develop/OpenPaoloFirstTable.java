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

        String tablePath = "/Users/tischer/Desktop/Paolo.txt";
        File tableFile = new File( tablePath );

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.table = tableFile;
        command.run();
    }
}
