package projects;

import de.embl.cba.mobie2.MoBIE;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenLocalTestData
{
	public static void main( String[] args ) throws IOException
	{
		final MoBIE moBIE = new MoBIE("/g/kreshuk/pape/Work/data/mobie/example-project");
	}
}
