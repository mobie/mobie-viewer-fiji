package repos;

import de.embl.cba.mobie.viewer.MoBIEViewer;

public class OpenCovidEM
{
	public static void main( String[] args )
	{
		// new ImageJ().ui().showUI();

		final MoBIEViewer moBIEViewer = new MoBIEViewer(
				"https://git.embl.de/pape/covid-em/-/raw/master/data",
				"https://git.embl.de/pape/covid-em/-/raw/master/data" );
	}
}
