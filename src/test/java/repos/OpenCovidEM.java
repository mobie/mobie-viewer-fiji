package repos;

import de.embl.cba.platynereis.platybrowser.MoBIEViewer;

public class OpenCovidEM
{
	public static void main( String[] args )
	{
		// new ImageJ().ui().showUI();

		final MoBIEViewer moBIEViewer = new MoBIEViewer(
				"Covid19-S4-Area2",
				"https://git.embl.de/pape/covid-em/-/raw/master/data",
				"https://git.embl.de/pape/covid-em/-/raw/master/data" );
	}
}
