package projects;

import de.embl.cba.mobie.ui.MoBIE;
import net.imagej.ImageJ;

public class OpenLocalAutophagosomesEM
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final MoBIE moBIE = new MoBIE("/g/kreshuk/pape/work/my_projects/autophagosoms-clem/data");
	}
}
