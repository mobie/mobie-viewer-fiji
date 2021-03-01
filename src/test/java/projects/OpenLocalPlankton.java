package projects;

import de.embl.cba.mobie.ui.MoBIEOptions;
import de.embl.cba.mobie.ui.ProjectManager;
import net.imagej.ImageJ;

public class OpenLocalPlankton
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();
		new ProjectManager("/Volumes/schwab/schwab/MoBIE-plankton", MoBIEOptions.options().imageDataStorageModality( MoBIEOptions.ImageDataStorageModality.FileSystem ));
	}
}
