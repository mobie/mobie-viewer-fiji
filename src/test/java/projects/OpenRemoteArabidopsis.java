package projects;

import org.embl.mobie.viewer.MoBIESettings;
import org.embl.mobie.viewer.MoBIE;
import net.imagej.ImageJ;

import java.io.IOException;
import org.embl.mobie.io.ImageDataFormat;

public class OpenRemoteArabidopsis
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		new MoBIE("https://github.com/mobie/arabidopsis-root-lm-datasets", MoBIESettings.settings().gitProjectBranch( "main" ).imageDataFormat( ImageDataFormat.BdvN5S3 ) );
	}
}
