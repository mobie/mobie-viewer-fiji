package org.embl.mobie.cmd;

import mpicbg.spim.data.SpimDataException;

import java.io.IOException;

class CommandLineMorphoLibJ2DTIFF
{
	public static final String ROOT = "/Users/tischer/Documents/mobie/";
	public static final String DIR = "src/test/resources/input/mlj-2d-tiff/";

	public static void main( String[] args ) throws Exception
	{
		final MoBIECmd cmd = new MoBIECmd();
		cmd.images = new String[]{ ROOT + DIR + "image.tif" };
		cmd.segmentations = new String[]{ ROOT + DIR + "segmentation.tif" };
		cmd.tables = new String[]{ ROOT + DIR + "table-mlj.csv" };
		cmd.call();
	}
}