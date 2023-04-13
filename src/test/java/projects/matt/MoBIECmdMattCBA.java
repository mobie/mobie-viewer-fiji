package projects.matt;

import org.embl.mobie.cmd.FilesCmd;

class MoBIECmdMattCBA
{
	public static void main( String[] args ) throws Exception
	{
		final FilesCmd cmd = new FilesCmd();
		cmd.root = "/Volumes/cba/exchange/matt-govendir/data";
		cmd.images = new String[]{ "preprocessed/.*.tif" };
		cmd.labels = new String[]{ "analysed/.*seg.tif" };
		cmd.tables = new String[]{ "analysed/.*_seg.csv" };
		cmd.call();
	}
}