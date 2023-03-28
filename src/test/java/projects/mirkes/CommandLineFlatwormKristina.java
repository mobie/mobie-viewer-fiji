package projects.mirkes;

import org.embl.mobie.cmd.MoBIECmd;

class CommandLineFlatwormKristina
{
	public static final String DIR = "/Users/tischer/Desktop/Kristina/2022_11_18/";

	public static void main( String[] args ) throws Exception
	{
		final MoBIECmd cmd = new MoBIECmd();
		cmd.root = DIR;
		cmd.images = new String[]{ ".*-pro.tif" };
		cmd.labels = new String[]{ ".*-seg.tif,.*.csv" };
		cmd.call();
	}
}