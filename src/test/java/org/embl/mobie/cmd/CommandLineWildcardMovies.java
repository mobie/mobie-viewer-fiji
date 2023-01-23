package org.embl.mobie.cmd;

import mpicbg.spim.data.SpimDataException;

import java.io.IOException;

class CommandLineWildcardMovies
{
	public static final String DIR = "/Users/tischer/Desktop/Kristina/output/";

	public static void main( String[] args ) throws SpimDataException, IOException
	{
		final MoBIECmd commandLineInterface = new MoBIECmd();
		commandLineInterface.run(
				new String[]{ DIR + "*-pro.tif" },
				new String[]{ DIR + "*-seg.tif" },
				new String[]{ DIR + "*.csv" }
				);
	}
}