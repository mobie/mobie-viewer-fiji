package projects;

import org.embl.mobie.viewer.MoBIESettings;
import org.embl.mobie.viewer.MoBIE;
import net.imagej.ImageJ;

import java.io.IOException;
import org.embl.mobie.io.ImageDataFormat;

public class OpenRemoteCLEMExample
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		new MoBIE("https://github.com/mobie/clem-example-project/", MoBIESettings.settings().gitProjectBranch( "more-views" ).view( "Fig2_a" ));
	}
}
