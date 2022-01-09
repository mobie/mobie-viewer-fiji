package projects;

import net.imagej.ImageJ;
import org.embl.mobie.io.n5.util.ImageDataFormat;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;

import java.io.IOException;

public class OpenLocalPlankton
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();
		try {
			new MoBIE("/Volumes/emcf/pape/plankton-fibsem-project",
					MoBIESettings.settings().imageDataFormat( ImageDataFormat.BdvN5 ).dataset( "galdieria" ) );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
