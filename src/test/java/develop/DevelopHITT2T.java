package develop;

import ij3d.Image3DUniverse;
import net.imagej.ImageJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.command.open.OpenCollectionTableCommand;
import org.embl.mobie.command.open.special.OpenCollectionTableExpertCommand;
import org.embl.mobie.lib.bdv.BdvViewingMode;

public class DevelopHITT2T
{
    public static void main( String[] args )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        //command.tableUri = "/Users/tischer/Desktop/hitt2t/Tischi-Mobie-Bspline/mobie.csv";
        //command.tableUri = "/Users/tischer/Desktop/hitt2t/Full-Res/mobie_ki.csv";
        //command.tableUri = "https://s3.embl.de/hitt2t/20251209/mm_Ki5_FFPE_20251209_S1/Mobie/Slide19/mobie_ki.csv";
        //command.tableUri = "/Users/tischer/Desktop/hitt2t/slide19-test/mobie_ki_tischi.csv";
        //command.tableUri = "/Volumes/cba/Abbaspour-Tischer/Hitt2t/Reg_3DLM_Fix_Xray_Mov/Mobie/Ki5/Slice6/mobie_ki.csv";
        command.tableUri = "/Users/tischer/Downloads/slide4_6_Embryo/mobie_slide4_6.csv";
        command.run();
    }
}
