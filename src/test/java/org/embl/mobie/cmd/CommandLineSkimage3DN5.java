package org.embl.mobie.cmd;

class CommandLineSkimage3DN5
{
	public static final String ROOT = "/Users/tischer/Documents/mobie/";
	public static final String DIR = "src/test/resources/input/skimage-3d-n5/";

	public static void main( String[] args ) throws Exception
	{
		final ProjectCmd cmd = new ProjectCmd();
		cmd.root = ROOT + DIR;
		cmd.images = new String[]{ "image.xml" };
		cmd.labels = new String[]{ "segmentation.xml,table.csv" };
		cmd.call();
	}
}