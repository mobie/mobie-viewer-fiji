package headless;

import de.embl.cba.platynereis.platybrowser.MoBIEViewer;
import net.imagej.ImageJ;

public class OpenPlatyBrowserUnsignedLong
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final MoBIEViewer moBIEViewer = new MoBIEViewer(
				"1.0.1",
				"/Volumes/arendt/EM_6dpf_segmentation/platy-browser-data/data",
				"/Volumes/arendt/EM_6dpf_segmentation/platy-browser-data/data" );
	}
}
