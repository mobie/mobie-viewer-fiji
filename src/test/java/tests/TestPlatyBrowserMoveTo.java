package tests;

import de.embl.cba.platynereis.platybrowser.PlatyBrowser;
import net.imagej.ImageJ;
import org.junit.Test;

public class TestPlatyBrowserMoveTo
{
//	@Test
	public void moveToPoint( )
	{
		new ImageJ().ui().showUI();

		final PlatyBrowser platyBrowser = new PlatyBrowser(
				"0.2.1",
				"/Volumes/arendt/EM_6dpf_segmentation/platy-browser-data/data",
				"https://git.embl.de/tischer/platy-browser-tables/raw/dev/data" );

		platyBrowser.getActionPanel().setView( "177.46666666666667,214.46666666666667,67.0" );
	}


	public static void main( String[] args )
	{
		new TestPlatyBrowserMoveTo().moveToPoint( );
	}
}
