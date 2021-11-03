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
            new MoBIE("/g/kreshuk/pape/work/my_projects/autophagosoms-clem/data",
                    MoBIESettings.settings().imageDataFormat( ImageDataFormat.BdvN5 ) );
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}
