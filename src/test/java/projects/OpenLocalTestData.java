package projects;

import net.imagej.ImageJ;
import org.embl.mobie.io.n5.util.ImageDataFormat;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;

import java.io.IOException;

public class OpenLocalTestData
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		final MoBIE moBIE = new MoBIE("/g/emcf/pape/mobie-test-projects", MoBIESettings.settings().imageDataFormat( ImageDataFormat.BdvN5 ));
	}
}
