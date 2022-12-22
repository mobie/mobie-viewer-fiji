package org.embl.mobie.cmd;

class MoBIECommandLineInterfaceTest
{
	public static final String ROOT = "/Users/tischer/Documents/mobie/";

	public static void main( String[] args )
	{
		final MoBIECommandLineInterface commandLineInterface = new MoBIECommandLineInterface();
		commandLineInterface.run(
				new String[]{ ROOT + "src/test/resources/golgi-intensities.tif" },
				new String[]{ ROOT + "src/test/resources/golgi-cell-labels.tif" } );
	}
}