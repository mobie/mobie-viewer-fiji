package projects;

import de.embl.cba.mobie.ui.viewer.MoBIEOptions;
import de.embl.cba.mobie.ui.viewer.MoBIEViewer;
import net.imagej.ImageJ;

public class OpenLocalPlankton
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();
		new MoBIEViewer("/Volumes/schwab/schwab/MoBIE-plankton", MoBIEOptions.options().imageDataStorageModality( MoBIEOptions.ImageDataStorageModality.FileSystem ));
	}
}
