package projects.govendir;

import org.embl.mobie.cmd.ProjectCmd;

class MoBIECmdMattCBAAnalysed
{
	public static void main( String[] args ) throws Exception
	{
		final ProjectCmd cmd = new ProjectCmd();
		cmd.root = "/Volumes/cba/exchange/matt-govendir/data";
		cmd.images = new String[]{ "preprocessed/(?<date>.*)__(?<treat>.*_.*)_(?<repl>.*)--mem.tif" };
		cmd.labels = new String[]{ "analysed/.*.tif" };
		cmd.call();
	}
}