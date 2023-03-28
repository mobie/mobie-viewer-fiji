package projects.govendir;

import org.embl.mobie.cmd.MoBIECmd;

class MoBIECmdMattCBA
{
	public static void main( String[] args ) throws Exception
	{
		// 2023_01_18--Extract_8hr_1_ch0
		final MoBIECmd cmd = new MoBIECmd();
		cmd.root = "/Volumes/cba/exchange/matt-govendir/data";
		cmd.images = new String[]{ "preprocessed/.*.tif" };
		cmd.labels = new String[]{ "analysed/.*seg.tif;analysed/.*_seg.csv" };
		cmd.call();
	}
}