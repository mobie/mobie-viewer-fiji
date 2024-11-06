package debug;

import net.imagej.ImageJ;

import org.embl.mobie.command.open.OpenOMEZARRCommand;

public class DebugFloatSourceBVV
{
	public static void main( String[] args )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenOMEZARRCommand command = new OpenOMEZARRCommand();
        command.image = "https://s3.embl.de/imatrec/ATH_20to200_20240703_PM_01_epo_02_P3_32bit.ome.zarr";
        command.run();
    }
}
