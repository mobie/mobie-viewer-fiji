package debug;

import net.imagej.ImageJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;

import java.io.IOException;

public class DebugIssue1044
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		new MoBIE("/Users/tischer/Downloads/minimal-mobie/minimal-mobie-project", MoBIESettings.settings().view( "segmentations" ) );// .getViewManager().show( "cell-segmentation" );

	}
}
