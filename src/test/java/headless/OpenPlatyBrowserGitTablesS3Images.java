package headless;

import de.embl.cba.platynereis.platybrowser.MoBIEViewer;
import net.imagej.ImageJ;

public class OpenPlatyBrowserGitTablesS3Images
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final MoBIEViewer moBIEViewer = new MoBIEViewer(
				"0.6.5",
				"https://git.embl.de/tischer/platy-browser-tables/raw/master/data/test_n5",
				"https://git.embl.de/tischer/platy-browser-tables/raw/master/data/test_n5" );
	}
}
