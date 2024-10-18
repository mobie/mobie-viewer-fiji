package debug;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenOMEZARRCommand;

public class DebugAngelikaOMEZarrIssue
{
    public static void main( String[] args )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenOMEZARRCommand command = new OpenOMEZARRCommand();
        command.image = "https://s3.embl.de/imatrec/IMATREC_HiTT_20240718_AS/TAL_10to40_20230622_AM_01_epo_02.ome.zarr";
        command.image = "https://s3.embl.de/imatrec/IMATREC_HiTT_20240501_AS/135.ome.zarr";
        command.run();
    }
}
