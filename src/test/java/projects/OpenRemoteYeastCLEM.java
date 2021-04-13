package projects;

import de.embl.cba.mobie.ui.MoBIEOptions;
import de.embl.cba.mobie2.MoBIE2;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenRemoteYeastCLEM
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		new MoBIE2("https://github.com/mobie/yeast-clem-datasets", MoBIEOptions.options().gitProjectBranch( "spec-v2" ).imageDataStorageModality( MoBIEOptions.ImageDataStorageModality.S3 ) );
	}
}
