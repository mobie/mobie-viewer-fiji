package org.embl.mobie.cmd;

import mpicbg.spim.data.SpimDataException;

class MoBIECommandLineInterfaceTest
{
	public static final String ROOT = "/Users/tischer/Documents/mobie/";

	public static void main( String[] args ) throws SpimDataException
	{
		final MoBIECommandLineInterface commandLineInterface = new MoBIECommandLineInterface();
		commandLineInterface.run(
				new String[]{ ROOT + "src/test/resources/golgi-intensities.tif" },
				new String[]{ ROOT + "src/test/resources/golgi-cell-labels.tif" },
				new String[]{ ROOT + "src/test/resources/golgi-cell-features-mlj.csv" }
				);
	}
}