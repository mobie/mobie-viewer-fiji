package org.embl.mobie.cmd;

import mpicbg.spim.data.SpimDataException;

import java.io.IOException;

class CommandLineSkimage2DTZarr
{
	public static final String ROOT = "/Users/tischer/Documents/mobie/";
	public static final String DIR = "src/test/resources/input/skimage-2dt-zarr/";

	public static void main( String[] args ) throws Exception
	{
		final MoBIECmd cmd = new MoBIECmd();
		cmd.images = new String[]{ ROOT + DIR + "image.ome.zarr" };
		cmd.segmentations = new String[]{ ROOT + DIR + "segmentation.ome.zarr" };
		cmd.tables = new String[]{ ROOT + DIR + "table.tsv" };
		cmd.call();
	}
}