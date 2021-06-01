package projects;

import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.MoBIESettings;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenRemotePlatynereis
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		try {
			new MoBIE("https://github.com/platybrowser/platybrowser",
					MoBIESettings.settings().imageDataStorageModality( MoBIESettings.ImageDataStorageModality.S3 ) );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
