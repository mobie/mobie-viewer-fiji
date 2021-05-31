package projects;

import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.MoBIESettings;
import net.imagej.ImageJ;

import java.io.IOException;

public class OpenLocalCovidEMCF
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		try {
			new MoBIE("/Volumes/emcf/common/5792_Sars-Cov-2/covid-em/data",
					MoBIESettings.settings().imageDataStorageModality( MoBIESettings.ImageDataStorageModality.FileSystem ) );
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
