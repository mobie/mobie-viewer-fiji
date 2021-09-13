package projects;

import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;
import org.embl.mobie.viewer.source.ImageDataFormat;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenRemoteZebrafishGridView
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		final MoBIE moBIE = new MoBIE( "https://github.com/mobie/zebrafish-lm-datasets", MoBIESettings.settings().gitProjectBranch( "new-grid-spec" ).imageDataFormat( ImageDataFormat.BdvN5S3 ) );

		// zooming in and out of the grid view feels sometimes stuck...
		moBIE.getViewManager().show( moBIE.getViews().get( "grid-view" ) );
	}
}
