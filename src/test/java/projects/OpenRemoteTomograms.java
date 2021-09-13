package projects;

import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;
import org.embl.mobie.viewer.source.ImageDataFormat;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenRemoteTomograms
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		try {
			new MoBIE("https://github.com/mobie/covid-tomo-datasets",
					MoBIESettings.settings().imageDataFormat( ImageDataFormat.BdvN5S3 ) );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
