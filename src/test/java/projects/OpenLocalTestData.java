package projects;

import de.embl.cba.mobie2.MoBIE2;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenLocalTestData
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		final MoBIE2 moBIE2 = new MoBIE2("/g/kreshuk/pape/Work/data/mobie/example-project");
	}
}
