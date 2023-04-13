package org.embl.mobie.cmd;

class CommandLineSkimage2DTZarr
{
	public static final String ROOT = "/Users/tischer/Documents/mobie/";

	public static void main( String[] args ) throws Exception
	{
		final FilesCmd cmd = new FilesCmd();
		cmd.root = ROOT + "src/test/resources/input/skimage-2dt-zarr/";
		cmd.images = new String[]{ "image.ome.zarr" };
		cmd.labels = new String[]{ "segmentation.ome.zarr" };
		cmd.tables = new String[]{ "table.tsv" };
		cmd.call();
	}
}