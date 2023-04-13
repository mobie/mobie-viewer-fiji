package projects.matt;

import org.embl.mobie.cmd.FilesCmd;

class MoBIECmdMattCBAAnalysed
{
	public static void main( String[] args ) throws Exception
	{
		final FilesCmd cmd = new FilesCmd();
		cmd.root = "/Volumes/cba/exchange/matt-govendir/data";
		cmd.images = new String[]{ "preprocessed/(?<date>.*)__(?<treat>.*_.*)_(?<repl>.*)--mem.tif" };
		cmd.labels = new String[]{ "analysed/.*.tif" };
		cmd.call();
	}
}