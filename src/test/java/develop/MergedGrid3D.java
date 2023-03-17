package develop;

import net.imagej.ImageJ;
import org.embl.mobie.MoBIE;

import java.io.IOException;

public class MergedGrid3D
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		try {
			new MoBIE("/Users/tischer/Downloads/example/mobie-example-project" );//.getViewManager().show( "cell-segmentation" );
		} catch ( IOException e) {
			e.printStackTrace();
		}

	}
}
