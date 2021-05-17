package projects;

import de.embl.cba.mobie.ui.MoBIESettings;
import de.embl.cba.mobie.ui.MoBIE;
import net.imagej.ImageJ;

public class OpenLocalPlankton
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();
		new MoBIE("/Volumes/schwab/schwab/MoBIE-plankton", MoBIESettings.settings().imageDataStorageModality( MoBIESettings.ImageDataStorageModality.FileSystem ));
	}
}
