package projects;

import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.MoBIESettings;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenRemotePlatynereisBranch
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		new MoBIE( "https://github.com/mobie/platybrowser-datasets", MoBIESettings.settings().gitProjectBranch( "spec-v2" ) );
	}
}
