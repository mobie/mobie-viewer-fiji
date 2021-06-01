package projects;

import de.embl.cba.mobie.MoBIESettings;
import de.embl.cba.mobie.MoBIE;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenRemoteYeastCLEM
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		new MoBIE("https://github.com/mobie/yeast-clem-datasets", MoBIESettings.settings().gitProjectBranch( "spec-v2" ).imageDataStorageModality( MoBIESettings.ImageDataStorageModality.S3 ) );
	}
}
