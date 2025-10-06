package issues;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenCollectionTableCommand;
import org.embl.mobie.lib.bdv.BdvViewingMode;

public class SpotsRenderingOnWindows
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = "/Users/tischer/Desktop/mobie-bug/collection.txt";
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseTableFolder;
        command.bdvViewingModeEnum = BdvViewingMode.TwoDimensional;
        command.run();
    }
}
