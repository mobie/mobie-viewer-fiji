package projects;

import de.embl.cba.mobie.ui.viewer.MoBIEViewer;
import de.embl.cba.mobie.ui.viewer.ViewerOptions;

public class OpenRemotePlatynereisBranch
{
	public static void main( String[] args )
	{
		new MoBIEViewer( "https://github.com/platybrowser/platybrowser", ViewerOptions.options().gitBranch( "master" ) );
	}
}
