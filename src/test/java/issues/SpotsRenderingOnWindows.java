package issues;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenCollectionTableExpertCommand;
import org.embl.mobie.lib.bdv.BdvViewingMode;

public class SpotsRenderingOnWindows
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();

        OpenCollectionTableExpertCommand command = new OpenCollectionTableExpertCommand();
        command.tableUri = "/Users/tischer/Desktop/mobie-bug/collection.txt";
        command.dataRootTypeEnum = OpenCollectionTableExpertCommand.DataRootType.UseTableFolder;
        command.bdvViewingModeEnum = BdvViewingMode.TwoDimensional;
        command.run();
    }
}
