package projects;

import de.embl.cba.mobie.ui.viewer.MoBIEViewer;
import mpicbg.spim.data.XmlIoSpimData;
import net.imagej.ImageJ;

public class OpenLocalAutophagosomesEM
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final MoBIEViewer moBIEViewer = new MoBIEViewer("/g/kreshuk/pape/work/my_projects/autophagosoms-clem/data");

		new XmlIoSpimData().save(  );
	}
}
