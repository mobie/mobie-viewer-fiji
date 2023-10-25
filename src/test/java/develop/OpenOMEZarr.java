package develop;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.omezarr.OpenOMEZARRCommand;

import java.io.File;

public class OpenOMEZarr
{
    public static void main( String[] args )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenOMEZARRCommand command = new OpenOMEZARRCommand();
        command.omeZarrDirectory = new File( "/Volumes/cba/exchange/ome-zarr/bugra/Result-Image-Zarr/Result-Image--WA01--P0001--T0001--001.ome.zarr" );
        command.run();
    }
}
