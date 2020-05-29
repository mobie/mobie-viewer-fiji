package projects;

import de.embl.cba.mobie.viewer.MoBIEViewer;
import net.imagej.ImageJ;

public class OpenLocalAutophagosomesEM
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final MoBIEViewer moBIEViewer = new MoBIEViewer(
				"/g/kreshuk/pape/work/my_projects/autophagosoms-clem/data",
				"https://github.com/constantinpape/autophagosomes-clem" );
	}
}
