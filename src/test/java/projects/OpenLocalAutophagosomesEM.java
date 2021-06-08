package projects;

import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.MoBIESettings;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenLocalAutophagosomesEM
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();
		try {
			new MoBIE("/g/kreshuk/pape/work/my_projects/autophagosoms-clem/data",
					MoBIESettings.settings().imageDataFormat( MoBIESettings.ImageDataFormat.BdvN5 ) );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
