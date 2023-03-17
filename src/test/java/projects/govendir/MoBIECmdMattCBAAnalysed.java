package projects.govendir;

import org.embl.mobie.cmd.MoBIECmd;

class MoBIECmdMattCBAAnalysed
{
	public static void main( String[] args ) throws Exception
	{
		final MoBIECmd cmd = new MoBIECmd();
		cmd.images = new String[]{ "/Volumes/cba/exchange/matt-govendir/data/preprocessed/*.tif" };
		cmd.segmentations = new String[]{"/Volumes/cba/exchange/matt-govendir/data/analysed/*.tif" };
		cmd.grids = new String[]{ "(?<date>*)__(?<treat>*_*)_(?<repl>*)--mem;treat", "(?<date>*)__(?<treat>*_*)_(?<repl>*)--mem_seg;treat", };
		cmd.call();
	}
}