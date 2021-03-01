package tests;

import de.embl.cba.mobie.ui.ProjectManager;
import net.imagej.ImageJ;

public class TestPlatyBrowserMoveTo
{
//	@Test
	public void moveToPoint( )
	{
		new ImageJ().ui().showUI();

		final ProjectManager projectManager = new ProjectManager(
				"/Volumes/arendt/EM_6dpf_segmentation/platy-browser-data/data",
				"https://git.embl.de/tischer/platy-browser-tables/raw/dev/data" );

		projectManager.getUserInterfacePanelsProvider().setView( "177.46666666666667,214.46666666666667,67.0" );
	}


	public static void main( String[] args )
	{
		new TestPlatyBrowserMoveTo().moveToPoint( );
	}
}
