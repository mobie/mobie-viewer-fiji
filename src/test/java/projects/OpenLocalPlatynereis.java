package projects;

import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.MoBIESettings;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenLocalPlatynereis
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		try {
			new MoBIE("/g/arendt/EM_6dpf_segmentation/platy-browser-data/data/",
					MoBIESettings.settings().imageDataStorageModality( MoBIESettings.ImageDataStorageModality.FileSystem ) );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
