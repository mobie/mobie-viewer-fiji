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

		// "new_crop" .view( "TEST_new_grid" ) "MMRR_06_Grid1_c442_c1_cropnew"

		try {
			new MoBIE("https://github.com/mobie/centrioles-tomo-datasets", MoBIESettings.settings().gitProjectBranch( "grid-test-tomo" ).imageDataFormat( ImageDataFormat.BdvN5S3 ) );
//			new MoBIE("https://github.com/mobie/centrioles-tomo-datasets", MoBIESettings.settings().gitProjectBranch( "new_crop" ).imageDataFormat( ImageDataFormat.BdvN5S3 ).view( "TEST_new_grid" ) );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
