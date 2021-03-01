package projects;

import de.embl.cba.mobie.ui.MoBIEOptions;
import de.embl.cba.mobie.ui.MoBIE;
import net.imagej.ImageJ;

public class OpenLocalPlankton
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();
		new MoBIE("/Volumes/schwab/schwab/MoBIE-plankton", MoBIEOptions.options().imageDataStorageModality( MoBIEOptions.ImageDataStorageModality.FileSystem ));
	}
}
