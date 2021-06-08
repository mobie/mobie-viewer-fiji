package projects;

import de.embl.cba.mobie.MoBIESettings;
import de.embl.cba.mobie.MoBIE;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenRemoteArabidopsis
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		new MoBIE("https://github.com/mobie/arabidopsis-root-lm-datasets", MoBIESettings.settings().gitProjectBranch( "new-data-spec3" ).imageDataFormat( MoBIESettings.ImageDataFormat.BdvN5S3 ) );
	}
}
