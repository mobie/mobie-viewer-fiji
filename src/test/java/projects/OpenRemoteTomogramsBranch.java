package projects;

import de.embl.cba.mobie.ui.MoBIESettings;
import de.embl.cba.mobie.ui.MoBIE;
import net.imagej.ImageJ;

public class OpenRemoteTomogramsBranch
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		new MoBIE("https://github.com/mobie/covid-tomo-datasets", MoBIESettings.settings().gitProjectBranch( "grid-test" )  );
	}
}
