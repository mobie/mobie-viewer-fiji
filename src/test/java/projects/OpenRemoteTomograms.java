package projects;

import de.embl.cba.mobie.ui.viewer.MoBIEViewer;
import net.imagej.ImageJ;

public class OpenRemoteTomograms
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		new MoBIEViewer("https://github.com/mobie/covid-tomo-datasets" );
	}
}
