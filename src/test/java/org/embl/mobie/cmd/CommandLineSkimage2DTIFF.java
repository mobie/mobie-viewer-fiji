package org.embl.mobie.cmd;

class CommandLineSkimage2DTIFF
{
	public static final String ROOT = "/Users/tischer/Documents/mobie/";
	public static final String DIR = "src/test/resources/input/skimage-2d-tiff/";

	public static void main( String[] args ) throws Exception
	{
		final MoBIECmd cmd = new MoBIECmd();
		cmd.root = ROOT + DIR;
		cmd.images = new String[]{ "image.tif" };
		cmd.labels = new String[]{ "segmentation.tif,table.tsv" };
		cmd.call();
	}
}