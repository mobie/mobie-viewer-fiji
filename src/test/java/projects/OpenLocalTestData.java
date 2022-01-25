package projects;

import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;
import net.imagej.ImageJ;

import java.io.IOException;
import org.embl.mobie.io.ImageDataFormat;

public class OpenLocalTestData
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		final MoBIE moBIE = new MoBIE("/g/emcf/pape/mobie-test-projects", MoBIESettings.settings().imageDataFormat( ImageDataFormat.BdvN5 ));
	}
}
