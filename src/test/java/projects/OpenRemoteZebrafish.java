package projects;

import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.MoBIESettings;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenRemoteZebrafish
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		new MoBIE("https://github.com/mobie/zebrafish-lm-datasets", MoBIESettings.settings().gitProjectBranch( "spec-v2" ).imageDataFormat( MoBIESettings.ImageDataFormat.BdvN5S3 ) );
	}
}
