package repos;

import de.embl.cba.platynereis.platybrowser.MoBIEViewer;
import net.imagej.ImageJ;

public class OpenAutophagosomesEM
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final MoBIEViewer moBIEViewer = new MoBIEViewer(
				"10spd",
				"/g/kreshuk/pape/work/my_projects/autophagosoms-clem/data",
				"https://github.com/constantinpape/autophagosomes-clem" );
	}
}
