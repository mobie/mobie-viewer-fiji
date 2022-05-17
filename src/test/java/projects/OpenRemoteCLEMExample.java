package projects;

import net.imagej.ImageJ;
import org.embl.mobie.viewer.MoBIE;

import java.io.IOException;

public class OpenRemoteCLEMExample
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		new MoBIE("https://github.com/mobie/clem-example-project/");
	}
}
