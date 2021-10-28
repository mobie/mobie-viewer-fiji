package projects;

import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;
import org.embl.mobie.viewer.source.ImageDataFormat;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenLocalAutophagosomesEM
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();
		try {
			new MoBIE("/home/katerina/Documents/embl/mnt2/kreshuk/pape/Work/mobie/arabidopsis-root-lm-datasets/data",
					MoBIESettings.settings().imageDataFormat( ImageDataFormat.BdvN5 ) );
//            new MoBIE( "/home/katerina/Documents/embl/mnt/kreshuk/pape/Work/mobie/covid-if-project/data", MoBIESettings.settings().imageDataFormat( ImageDataFormat.OmeZarr ) );


        } catch (IOException e) {
			e.printStackTrace();
		}
	}
}
