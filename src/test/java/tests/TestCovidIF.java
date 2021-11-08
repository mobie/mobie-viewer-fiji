package tests;

import net.imagej.ImageJ;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;
import org.embl.mobie.viewer.source.ImageDataFormat;
import org.embl.mobie.viewer.view.View;
import org.embl.mobie.viewer.view.additionalviews.AdditionalViewsLoader;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

public class TestCovidIF
{
	public static void main( String[] args ) throws IOException
	{
		new TestCovidIF().testDefaultView();
	}

	@Test
	public void testDefaultView() throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		final MoBIE moBIE = new MoBIE( "https://github.com/mobie/covid-if-project", MoBIESettings.settings().gitProjectBranch( "main" ).imageDataFormat( ImageDataFormat.OmeZarr ) );
	}
}
