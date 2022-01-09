package projects;

import net.imagej.ImageJ;
import org.embl.mobie.io.n5.util.ImageDataFormat;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;

import java.io.IOException;

public class OpenLocalGiuliaMartinCLEM
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		new MoBIE("/g/emcf/pape/clem-example-project", MoBIESettings.settings().imageDataFormat( ImageDataFormat.BdvN5 ) );
	}
}
