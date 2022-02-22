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

		// .gitProjectBranch( "grid-test-tomo" )

		try {
			new MoBIE("https://github.com/mobie/centrioles-tomo-datasets", MoBIESettings.settings().gitProjectBranch( "grid-test-tomo" ).imageDataFormat( ImageDataFormat.BdvN5S3 ).view( "MMRR_06_Grid1_c442_c1_crop" ) );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
