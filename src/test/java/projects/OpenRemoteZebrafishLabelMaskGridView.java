package projects;

import de.embl.cba.mobie.ui.MoBIE;
import de.embl.cba.mobie.ui.MoBIEOptions;
import net.imagej.ImageJ;

public class OpenRemoteZebrafishLabelMaskGridView
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		final MoBIEOptions options = MoBIEOptions.options();
		options.dataset( "membrane" );

		new MoBIE("https://github.com/mobie/zebrafish-lm-datasets", options );
	}
}
