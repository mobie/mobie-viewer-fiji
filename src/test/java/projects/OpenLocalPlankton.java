package projects;

import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;
import net.imagej.ImageJ;

import java.io.IOException;
import org.embl.mobie.io.ImageDataFormat;

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
