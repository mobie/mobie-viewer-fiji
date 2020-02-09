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
		final PlatyBrowser platyBrowser = new PlatyBrowser(
				args[0],
				args[1],
				args[1] );
	}
}
