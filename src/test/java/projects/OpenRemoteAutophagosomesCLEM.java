package projects;

import de.embl.cba.mobie.viewer.MoBIEViewer;
import net.imagej.ImageJ;

public class OpenRemoteAutophagosomesCLEM
{
	public static void main( String[] args )
	{
		new MoBIEViewer( "https://github.com/mobie-org/autophagosomes-clem-datasets");
	}
}
