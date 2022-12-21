package org.embl.mobie.cmd;

class MoBIECommandLineInterfaceTest
{
	public static final String ROOT = "/Users/tischer/Documents/mobie/";

	public static void main( String[] args )
	{
		final MoBIECommandLineInterface commandLineInterface = new MoBIECommandLineInterface();
		commandLineInterface.run( ROOT + "src/test/resources/golgi-intensities.tif", ROOT + "src/test/resources/golgi-cell-labels.tif" );
	}
}