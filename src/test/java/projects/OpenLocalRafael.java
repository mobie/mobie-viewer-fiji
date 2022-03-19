package projects;

import net.imagej.ImageJ;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;

import java.io.IOException;

public class OpenLocalRafael
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		try {
			new MoBIE("/Users/tischer/Desktop/rafael/mobie",
					MoBIESettings.settings().imageDataFormat( ImageDataFormat.OmeZarr ) );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
