package projects;

import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenLocalCovidTomos
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		try {
			new MoBIE("/Volumes/kreshuk/pape/Work/mobie/covid-tomo-datasets", MoBIESettings.settings().imageDataFormat( ImageDataFormat.BdvN5 ) );
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
