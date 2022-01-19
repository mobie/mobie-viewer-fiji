package projects;

import net.imagej.ImageJ;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;

import java.io.IOException;
import org.embl.mobie.io.ImageDataFormat;

public class OpenRemoteCOMULIS
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		new MoBIE("https://s3.embl.de/comulis", MoBIESettings.settings().imageDataFormat( ImageDataFormat.BdvOmeZarrS3 ).s3AccessAndSecretKey( new String[]{"abc","def"} ) );
	}
}
