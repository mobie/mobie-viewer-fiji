package repos;

import de.embl.cba.platynereis.platybrowser.MoBIEViewer;

public class OpenPlatynereis
{
	public static void main( String[] args )
	{
		final MoBIEViewer moBIEViewer = new MoBIEViewer(
				"https://raw.githubusercontent.com/platybrowser/platybrowser/master/data",
				"https://raw.githubusercontent.com/platybrowser/platybrowser/master/data" );
	}
}
