package org.embl.mobie.cmd;

import mpicbg.spim.data.SpimDataException;

import java.io.IOException;

class CommandLineMorphoLibJ2DTIFF
{
	public static final String ROOT = "/Users/tischer/Documents/mobie/";
	public static final String DIR = "src/test/resources/input/mlj-2d-tiff/";

	public static void main( String[] args ) throws SpimDataException, IOException
	{
		final MoBIECmd commandLineInterface = new MoBIECmd();
		commandLineInterface.run(
				new String[]{ ROOT + DIR + "image.tif" },
				new String[]{ ROOT + DIR + "segmentation.tif" },
				new String[]{ ROOT + DIR + "table-mlj.csv" }
				);
	}
}