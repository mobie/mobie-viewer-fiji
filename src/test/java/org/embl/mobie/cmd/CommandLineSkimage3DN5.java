package org.embl.mobie.cmd;

import mpicbg.spim.data.SpimDataException;

class CommandLineSkimage3DN5
{
	public static final String ROOT = "/Users/tischer/Documents/mobie/";
	public static final String DIR = "src/test/resources/input/skimage-3d-n5/";

	public static void main( String[] args ) throws SpimDataException
	{
		final MoBIECmd commandLineInterface = new MoBIECmd();
		commandLineInterface.run(
				new String[]{ ROOT + DIR + "image.xml" },
				new String[]{ ROOT + DIR + "segmentation.xml" },
				new String[]{ ROOT + DIR + "table.tsv" }
				);
	}
}