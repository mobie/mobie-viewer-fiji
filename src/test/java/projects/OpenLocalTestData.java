package projects;

import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.MoBIESettings;
import de.embl.cba.mobie.source.ImageDataFormat;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenLocalTestData
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		final MoBIE moBIE = new MoBIE("/g/emcf/pape/mobie-test-projects", MoBIESettings.settings().imageDataFormat( ImageDataFormat.BdvN5 ));
	}
}
