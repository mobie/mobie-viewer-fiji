package org.embl.mobie.cmd;

class CommandLineMorphoLibJ2DTIFF
{
	public static final String ROOT = "/Users/tischer/Documents/mobie/";
	public static final String DIR = "src/test/resources/input/mlj-2d-tiff/";

	public static void main( String[] args ) throws Exception
	{
		final ProjectCmd cmd = new ProjectCmd();
		cmd.root = ROOT + DIR;
		cmd.images = new String[]{ "image.tif" };
		cmd.labels = new String[]{ "segmentation.tif,table-mlj.csv" };
		cmd.call();
	}
}