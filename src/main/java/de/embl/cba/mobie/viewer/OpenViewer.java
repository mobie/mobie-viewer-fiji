package de.embl.cba.mobie.viewer;

public class OpenViewer
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
				args[1],
				args[1] );
	}
}
