package projects;

import de.embl.cba.mobie.ui.ProjectManager;
import net.imagej.ImageJ;

public class OpenRemotePlatynereis
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		new ProjectManager("https://github.com/platybrowser/platybrowser" );
	}
}
