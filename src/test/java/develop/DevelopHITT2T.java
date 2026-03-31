package develop;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.special.OpenCollectionTableExpertCommand;
import org.embl.mobie.lib.bdv.BdvViewingMode;

public class DevelopHITT2T
{
    public static void main( String[] args )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableExpertCommand command = new OpenCollectionTableExpertCommand();
        command.tableUri = "/Users/tischer/Desktop/hitt2t/mobie-s3.xlsx";
        command.dataRootTypeEnum = OpenCollectionTableExpertCommand.DataRootType.PathsInTableAreAbsolute;
        command.bdvViewingModeEnum = BdvViewingMode.ThreeDimensional;
        command.run();
    }
}
