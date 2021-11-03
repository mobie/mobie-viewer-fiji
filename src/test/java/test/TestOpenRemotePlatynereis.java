package test;

import ij.IJ;
import net.imagej.ImageJ;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;
import org.embl.mobie.viewer.view.View;

import java.io.IOException;
import java.util.Map;

public class TestOpenRemotePlatynereis
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		final MoBIE moBIE = new MoBIE( "https://github.com/mobie/platybrowser-datasets", MoBIESettings.settings().gitProjectBranch( "spec-v2" ) );
		final Map< String, View > views = moBIE.getViews();
		final View view = views.get( "Figure 2B: Epithelial cell segmentation" );
		moBIE.getViewManager().show( view );
	}
}
