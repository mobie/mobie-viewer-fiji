package projects;

import net.imagej.ImageJ;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;

import java.io.IOException;

public class OpenLocalCOMULIS
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		new MoBIE("/Volumes/cba/exchange/ome-zarr/mobie/comulis", MoBIESettings.settings().imageDataFormat( ImageDataFormat.OmeZarr ) );

	}
}
