package org.embl.mobie.cmd;

class CommandLineWildcardMovies
{
	public static final String DIR = "/Users/tischer/Desktop/Kristina/2022_11_18/";

	public static void main( String[] args ) throws Exception
	{
		final MoBIECmd cmd = new MoBIECmd();
		cmd.images = new String[]{ DIR + "*-pro.tif" };
		cmd.segmentations = new String[]{ DIR + "*-seg.tif" };
		cmd.tables = new String[]{ DIR + "*.csv" };
		cmd.call();
	}
}