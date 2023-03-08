package projects.govendir;

import net.imagej.ImageJ;
import org.embl.mobie.cmd.MoBIECmd;

class MoBIECmdMattCBA
{
	public static void main( String[] args ) throws Exception
	{
		// 2023_01_18--Extract_8hr_1_ch0
		final MoBIECmd cmd = new MoBIECmd();
		cmd.images = new String[]{ "/Volumes/cba/exchange/matt-govendir/data/preprocessed/*.tif" };
		cmd.segmentations = new String[]{ "/Volumes/cba/exchange/matt-govendir/data/analysed/*seg.tif" };
		cmd.tables = new String[]{ "/Volumes/cba/exchange/matt-govendir/data/analysed/*_seg.csv" };
		cmd.grids = new String[]{ "*_1_ch*", "*_seg"};
		cmd.call();
	}
}