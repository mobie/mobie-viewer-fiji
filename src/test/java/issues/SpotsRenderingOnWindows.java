package issues;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenCollectionTableCommand;
import org.embl.mobie.lib.bdv.BdvViewingMode;

import java.io.File;

public class SpotsRenderingOnWindows
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.table = new File("/Users/tischer/Desktop/mobie-bug/collection.txt");
        command.dataRootType = OpenCollectionTableCommand.DataRoot.UseTableFolder;
        command.bdvViewingMode = BdvViewingMode.TwoDimensional;
        command.run();
    }
}
