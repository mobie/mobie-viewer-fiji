package projects;

import net.imagej.ImageJ;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;

import java.io.IOException;

public class OpenRemotePlankton
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		try {
			new MoBIE("https://github.com/mobie/plankton-fibsem-project", MoBIESettings.settings().dataset( "micromonas" ) );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
