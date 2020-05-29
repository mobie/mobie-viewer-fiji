package projects;

import de.embl.cba.mobie.viewer.MoBIEViewer;
import de.embl.cba.mobie.viewer.ViewerOptions;

public class OpenRemotePlatynereisBranch
{
	public static void main( String[] args )
	{
		new MoBIEViewer( "https://github.com/platybrowser/platybrowser", ViewerOptions.options().gitBranch( "master" ) );
	}
}
