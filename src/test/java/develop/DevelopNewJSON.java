package develop;

import net.imagej.ImageJ;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;

import java.io.IOException;

public class DevelopNewJSON
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		new MoBIE("https://github.com/mobie/clem-example-project/", MoBIESettings.settings().gitProjectBranch( "rename-test" ).view( "Figure2a" ));
	}
}
