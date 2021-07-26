package projects;

import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.MoBIESettings;
import de.embl.cba.mobie.source.ImageDataFormat;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenLocalCovidPlate
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		try {
			new MoBIE("/g/kreshuk/pape/Work/mobie/covid-if-project/data",
					MoBIESettings.settings().imageDataFormat( ImageDataFormat.OmeZarr ) );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
