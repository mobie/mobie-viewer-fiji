package de.embl.cba.platynereis.platybrowser;

public class OpenBrowser
{
	/**
	 * Open PlatyBrowser from command line
	 *
	 *
	 * @param args
	 */
	public static void main( String[] args )
	{
		final MoBIEViewer moBIEViewer = new MoBIEViewer(
				args[0],
				null, args[1],
				args[1] );
	}
}
