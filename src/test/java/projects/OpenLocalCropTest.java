package projects;

import de.embl.cba.mobie2.MoBIE2;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenLocalCropTest
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		final MoBIE2 moBIE2 = new MoBIE2("/g/emcf/pape/mobie-test-projects/mobie_crop");
	}
}
