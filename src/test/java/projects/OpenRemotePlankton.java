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
			new MoBIE("https://s3.embl.de/plankton-fibsem", MoBIESettings.settings());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
