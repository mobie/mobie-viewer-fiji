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
        //command.tableUri = "/Users/tischer/Desktop/hitt2t/Full-Res/mobie_ki.csv";
        command.tableUri = "https://s3.embl.de/hitt2t/20251209/mm_Ki5_FFPE_20251209_S1/Mobie/Slide19/mobie_ki.csv";
        command.tableUri = "/Users/tischer/Desktop/hitt2t/slide19-test/mobie_ki_tischi.csv";
        command.dataRootTypeEnum = OpenCollectionTableExpertCommand.DataRootType.PathsInTableAreAbsolute;
        command.bdvViewingModeEnum = BdvViewingMode.ThreeDimensional;
        command.run();
    }
}
