package org.embl.mobie.cmd;

class CommandLineSkimage2DTZarr
{
	public static final String ROOT = "/Users/tischer/Documents/mobie/";
	public static final String DIR = "src/test/resources/input/skimage-2dt-zarr/";

	public static void main( String[] args ) throws Exception
	{
		final MoBIECmd cmd = new MoBIECmd();
		cmd.root = ROOT + DIR;
		cmd.images = new String[]{ "image.ome.zarr" };
		cmd.labels = new String[]{ "segmentation.ome.zarr,table.tsv" };
		cmd.call();
	}
}