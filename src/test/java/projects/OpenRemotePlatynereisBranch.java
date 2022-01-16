package projects;

import net.imagej.ImageJ;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;

import java.io.IOException;

public class OpenRemotePlatynereisBranch
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		new MoBIE( "https://github.com/mobie/platybrowser-datasets", MoBIESettings.settings().gitProjectBranch( "normal-vie" ) );
	}
}
