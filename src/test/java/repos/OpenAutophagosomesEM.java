package repos;

import de.embl.cba.platynereis.platybrowser.MoBIEViewer;

public class OpenAutophagosomesEM
{
	public static void main( String[] args )
	{
		// new ImageJ().ui().showUI();

		final MoBIEViewer moBIEViewer = new MoBIEViewer(
				"10-spd",
				"https://raw.githubusercontent.com/constantinpape/autophagosomes-clem/master/data",
				"https://raw.githubusercontent.com/constantinpape/autophagosomes-clem/master/data" );
	}
}
