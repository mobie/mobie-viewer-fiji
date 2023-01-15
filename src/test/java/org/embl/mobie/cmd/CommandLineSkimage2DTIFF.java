package org.embl.mobie.cmd;

import mpicbg.spim.data.SpimDataException;

class CommandLineSkimage2DTIFF
{
	public static final String ROOT = "/Users/tischer/Documents/mobie/";
	public static final String DIR = "src/test/resources/input/skimage-2d-tiff/";

	public static void main( String[] args ) throws SpimDataException
	{
		final MoBIECmd commandLineInterface = new MoBIECmd();
		commandLineInterface.run(
				new String[]{ ROOT + DIR + "image.tif" },
				new String[]{ ROOT + DIR + "segmentation.tif" },
				new String[]{ ROOT + DIR + "table.tsv" }
				);
	}
}