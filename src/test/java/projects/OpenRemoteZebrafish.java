package projects;

import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;
import org.embl.mobie.viewer.source.ImageDataFormat;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenRemoteZebrafish
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		new MoBIE("https://github.com/mobie/zebrafish-lm-datasets", MoBIESettings.settings().gitProjectBranch( "spec-v2" ).imageDataFormat( ImageDataFormat.BdvN5S3 ) );
	}
}
