package projects;

import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.MoBIESettings;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenLocalCropTest
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		final MoBIE moBIE2 = new MoBIE("/g/emcf/pape/mobie-test-projects/mobie_crop", MoBIESettings.settings().imageDataStorageModality( MoBIESettings.ImageDataStorageModality.FileSystem ));
	}
}
