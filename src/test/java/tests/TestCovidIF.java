package tests;

import net.imagej.ImageJ;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;

import java.io.IOException;

public class TestCovidIF
{
	public static void main( String[] args ) throws IOException
	{
		new TestCovidIF().testDefaultView();
	}

	//@Test
	public void testDefaultView() throws IOException
	{
		// TODO: Bug loading the multi-scales
//		final ImageJ imageJ = new ImageJ();
//		imageJ.ui().showUI();
//		final MoBIE moBIE = new MoBIE( "https://github.com/mobie/covid-if-project", MoBIESettings.settings().gitProjectBranch( "main" ).imageDataFormat( ImageDataFormat.OmeZarr ) );
	}
}
