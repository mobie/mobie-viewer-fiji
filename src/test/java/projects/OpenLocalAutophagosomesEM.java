package projects;

import de.embl.cba.mobie.ui.ProjectManager;
import net.imagej.ImageJ;

public class OpenLocalAutophagosomesEM
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final ProjectManager projectManager = new ProjectManager("/g/kreshuk/pape/work/my_projects/autophagosoms-clem/data");
	}
}
