package org.embl.mobie.cmd;

class CommandLineSkimage3DN5
{
	public static final String ROOT = "/Users/tischer/Documents/mobie/";

	public static void main( String[] args ) throws Exception
	{
		final FilesCmd cmd = new FilesCmd();
		cmd.root = ROOT + "src/test/resources/input/skimage-3d-n5/";
		cmd.images = new String[]{ "image.xml" };
		cmd.labels = new String[]{ "segmentation.xml" };
		cmd.tables = new String[]{ "table.csv" };
		cmd.call();
	}
}