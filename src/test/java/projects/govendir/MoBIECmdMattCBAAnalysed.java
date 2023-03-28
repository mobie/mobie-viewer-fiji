package projects.govendir;

import org.embl.mobie.cmd.MoBIECmd;

class MoBIECmdMattCBAAnalysed
{
	public static void main( String[] args ) throws Exception
	{
		final MoBIECmd cmd = new MoBIECmd();
		cmd.root = "/Volumes/cba/exchange/matt-govendir/data";
		cmd.images = new String[]{ "preprocessed/(?<date>.*)__(?<treat>.*_.*)_(?<repl>.*)--mem.tif" };
		cmd.labels = new String[]{ "analysed/.*.tif" };
		cmd.call();
	}
}