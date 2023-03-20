package org.embl.mobie.cmd;

class CommandLineSkimage2DTIFF
{
	public static final String ROOT = "/Users/tischer/Documents/mobie/";
	public static final String DIR = "src/test/resources/input/skimage-2d-tiff/";

	public static void main( String[] args ) throws Exception
	{
		final MoBIECmd cmd = new MoBIECmd();
		cmd.images = new String[]{ ROOT + DIR + "image.tif" };
		cmd.labels = new String[]{ ROOT + DIR + "segmentation.tif" };
		cmd.tables = new String[]{ ROOT + DIR + "table.tsv" };
		cmd.call();
	}
}