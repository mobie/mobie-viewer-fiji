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
        command.tableUri = "/Users/tischer/Desktop/hitt2t/Tischi-Mobie-Bspline/mobie.csv";
        command.tableUri = "/Users/tischer/Desktop/hitt2t/Full-Res/mobie_ki.csv";
        command.dataRootTypeEnum = OpenCollectionTableExpertCommand.DataRootType.PathsInTableAreAbsolute;
        command.bdvViewingModeEnum = BdvViewingMode.ThreeDimensional;
        command.run();
    }
}
