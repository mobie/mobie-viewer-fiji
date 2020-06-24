package tests;

import de.embl.cba.mobie.ui.viewer.MoBIEViewer;
import net.imagej.ImageJ;

public class TestPlatyBrowserMoveTo
{
//	@Test
	public void moveToPoint( )
	{
		new ImageJ().ui().showUI();

		final MoBIEViewer moBIEViewer = new MoBIEViewer(
				"/Volumes/arendt/EM_6dpf_segmentation/platy-browser-data/data",
				"https://git.embl.de/tischer/platy-browser-tables/raw/dev/data" );

		moBIEViewer.getActionPanel().setView( "177.46666666666667,214.46666666666667,67.0" );
	}


	public static void main( String[] args )
	{
		new TestPlatyBrowserMoveTo().moveToPoint( );
	}
}
