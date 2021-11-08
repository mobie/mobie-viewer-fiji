package tests;

import ij.IJ;
import net.imagej.ImageJ;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;
import org.embl.mobie.viewer.source.ImageDataFormat;
import org.embl.mobie.viewer.view.View;
import org.embl.mobie.viewer.view.additionalviews.AdditionalViewsLoader;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

public class TestRemoteZebrafish
{
	public static void main( String[] args ) throws IOException
	{
		//new TestRemoteZebrafish().testSmallGridView();
		//new TestRemoteZebrafish().testMergedGridView();
		new TestRemoteZebrafish().testTransformedGridView();
	}

	@Test
	public void testSmallGridView() throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		final MoBIE moBIE = new MoBIE( "https://github.com/mobie/zebrafish-lm-datasets", MoBIESettings.settings().gitProjectBranch( "main" ).imageDataFormat( ImageDataFormat.BdvN5S3 ) );

		final Map< String, View > views = moBIE.getViews();
		moBIE.getViewManager().show( views.get( "small-grid-view" ) );
	}

	@Test
	public void testMergedGridView() throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		final MoBIE moBIE = new MoBIE( "https://github.com/mobie/zebrafish-lm-datasets", MoBIESettings.settings().gitProjectBranch( "main" ).imageDataFormat( ImageDataFormat.BdvN5S3 ) );

		final AdditionalViewsLoader viewsLoader = new AdditionalViewsLoader( moBIE );
		viewsLoader.loadViews( "https://raw.githubusercontent.com/mobie/zebrafish-lm-datasets/main/data/membrane/misc/views/test_views.json" );
		moBIE.getViewManager().show( "merged-grid"  );
	}

	@Test
	public void testTransformedGridView() throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		final MoBIE moBIE = new MoBIE( "https://github.com/mobie/zebrafish-lm-datasets", MoBIESettings.settings().gitProjectBranch( "main" ).imageDataFormat( ImageDataFormat.BdvN5S3 ) );

		final AdditionalViewsLoader viewsLoader = new AdditionalViewsLoader( moBIE );
		viewsLoader.loadViews( "https://raw.githubusercontent.com/mobie/zebrafish-lm-datasets/main/data/membrane/misc/views/test_views.json" );
		moBIE.getViewManager().show( "transformed-grid" );

		// TODO: add the focussing of one element in the table! This currently throws an error!
	}
}
