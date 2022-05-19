package projects;

import net.imagej.ImageJ;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;

import java.io.IOException;
import java.util.Map;

import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.viewer.view.View;

public class OpenRemoteCovidIF
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		final MoBIE moBIE = new MoBIE( "https://github.com/mobie/covid-if-project" );

	}
}
