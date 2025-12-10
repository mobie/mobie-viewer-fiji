package develop;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenCollectionTableCommand;
import org.embl.mobie.lib.bdv.BdvViewingMode;

public class DevelopHITT2T
{
    public static void main( String[] args )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = "/Users/tischer/Desktop/hitt2t/mobie-s3.xlsx";
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.PathsInTableAreAbsolute;
        command.bdvViewingModeEnum = BdvViewingMode.ThreeDimensional;
        command.run();
    }
}
