package projects;

import de.embl.cba.mobie.MoBIE;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenLocalTestData
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		final MoBIE moBIE2 = new MoBIE("/g/kreshuk/pape/Work/data/mobie/full-example");
	}
}
