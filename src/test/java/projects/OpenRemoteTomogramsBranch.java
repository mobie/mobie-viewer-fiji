package projects;

import de.embl.cba.mobie.ui.viewer.MoBIEOptions;
import de.embl.cba.mobie.ui.viewer.MoBIEViewer;
import net.imagej.ImageJ;

public class OpenRemoteTomogramsBranch
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		new MoBIEViewer("https://github.com/mobie/covid-tomo-datasets", MoBIEOptions.options().gitProjectBranch( "grid-test" )  );
	}
}
