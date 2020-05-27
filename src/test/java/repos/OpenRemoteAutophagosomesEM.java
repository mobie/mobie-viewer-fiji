package repos;

import de.embl.cba.mobie.viewer.MoBIEViewer;
import net.imagej.ImageJ;

public class OpenRemoteAutophagosomesEM
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final MoBIEViewer moBIEViewer = new MoBIEViewer(
				"https://github.com/constantinpape/autophagosomes-clem",
				"https://github.com/constantinpape/autophagosomes-clem" );
	}
}
