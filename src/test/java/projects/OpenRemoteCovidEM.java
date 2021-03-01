package projects;

import de.embl.cba.mobie.ui.ProjectManager;

public class OpenRemoteCovidEM
{
	public static void main( String[] args )
	{
		new ProjectManager( "https://github.com/mobie-org/covid-em-datasets");
	}
}
