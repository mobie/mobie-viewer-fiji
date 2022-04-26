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
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		try {
			final MoBIE moBIE = new MoBIE( "https://github.com/mobie/covid-if-project", MoBIESettings.settings().gitProjectBranch( "main" ).imageDataFormat( ImageDataFormat.OmeZarrS3 ).view( "merge-grid-no-tables" ) );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
