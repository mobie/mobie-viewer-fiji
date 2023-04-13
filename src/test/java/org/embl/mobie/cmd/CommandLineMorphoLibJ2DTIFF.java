package org.embl.mobie.cmd;

class CommandLineMorphoLibJ2DTIFF
{
	public static final String ROOT = "/Users/tischer/Documents/mobie/";

	public static void main( String[] args ) throws Exception
	{
		final FilesCmd cmd = new FilesCmd();
		cmd.root = ROOT + "src/test/resources/input/mlj-2d-tiff/";
		cmd.images = new String[]{ "image.tif" };
		cmd.labels = new String[]{ "segmentation.tif" };
		cmd.tables = new String[]{ "table-mlj.csv" };
		cmd.call();
	}
}