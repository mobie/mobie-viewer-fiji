package tests;

import de.embl.cba.mobie.bdv.BdvViewChanger;
import de.embl.cba.mobie.bookmark.Location;
import de.embl.cba.mobie.ui.MoBIE;
import net.imagej.ImageJ;

public class TestPlatyBrowserMoveTo
{
//	@Test
	public void moveToPoint( )
	{
		new ImageJ().ui().showUI();

		final MoBIE moBIE = new MoBIE(
				"/Volumes/arendt/EM_6dpf_segmentation/platy-browser-data/data",
				"https://git.embl.de/tischer/platy-browser-tables/raw/dev/data" );

		BdvViewChanger.moveToLocation( moBIE.getSourcesDisplayManager().getBdv(), new Location( "177.46666666666667,214.46666666666667,67.0" ) );
	}

	public static void main( String[] args )
	{
		new TestPlatyBrowserMoveTo().moveToPoint( );
	}
}
