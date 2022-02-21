package projects;

import net.imagej.ImageJ;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;

import java.io.IOException;

public class OpenRemoteCentriolesBranchCrop
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		try {
			new MoBIE("https://github.com/mobie/centrioles-tomo-datasets",
					MoBIESettings.settings().gitProjectBranch( "grid-test-tomo" ).imageDataFormat( ImageDataFormat.BdvN5S3 ).view( "B-ALL_00_Grid5_c0008_c1_crop" ) );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
