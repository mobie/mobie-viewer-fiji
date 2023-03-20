package org.embl.mobie.cmd;

class CommandLineSkimage3DN5
{
	public static final String ROOT = "/Users/tischer/Documents/mobie/";
	public static final String DIR = "src/test/resources/input/skimage-3d-n5/";

	public static void main( String[] args ) throws Exception
	{
		final MoBIECmd cmd = new MoBIECmd();
		cmd.images = new String[]{ ROOT + DIR + "image.xml" };
		cmd.labels = new String[]{ ROOT + DIR + "segmentation.xml" };
		cmd.tables = new String[]{ ROOT + DIR + "table.tsv" };
		cmd.call();
	}
}