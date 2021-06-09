package projects;

import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.MoBIESettings;
import de.embl.cba.mobie.source.ImageDataFormat;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenRemoteZebrafish
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		new MoBIE("https://github.com/mobie/zebrafish-lm-datasets", MoBIESettings.settings().gitProjectBranch( "new-data-spec" ).imageDataFormat( ImageDataFormat.BdvN5S3 ) );
	}
}
