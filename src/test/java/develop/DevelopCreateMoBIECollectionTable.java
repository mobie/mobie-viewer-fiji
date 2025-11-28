package develop;

import net.imagej.ImageJ;
import org.embl.mobie.command.create.CreateMoBIECollectionTableCommand;

import java.io.File;

public class DevelopCreateMoBIECollectionTable
{
    public static void main( String[] args )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        CreateMoBIECollectionTableCommand createCommand = new CreateMoBIECollectionTableCommand();
        //File dir = new File("/Users/tischer/Desktop/gautam-hiral/");
        File dir = new File("/Volumes/1D - NHS and Tub/NHS-TUB");
        File[] imgFiles = dir.listFiles((d, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".tif") || lower.endsWith(".tiff") || lower.endsWith(".nd2");
        });
        createCommand.files = imgFiles;
        createCommand.outputTableFile = new File("/Users/tischer/Desktop/gautam-hiral/mobie-collection-2.csv");
        createCommand.openTableInMoBIE = true;
        createCommand.run();
    }
}
