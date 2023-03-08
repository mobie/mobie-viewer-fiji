package org.embl.mobie.cmd;

import static org.junit.jupiter.api.Assertions.*;

class MoBIECmdTestMatt
{
	public static void main( String[] args ) throws Exception
	{
		// 2023_01_18--Extract_8hr_1_ch0
		final MoBIECmd cmd = new MoBIECmd();
		cmd.images = new String[]{ "/Users/tischer/Desktop/matt/preprocessed/*.tif" };
		cmd.segmentations = new String[]{ "/Users/tischer/Desktop/matt/analysed/*_seg.tif" };
		cmd.tables = new String[]{ "/Users/tischer/Desktop/matt/analysed/*_seg.csv" };
		// if it is like this "*_1*" one would expect 4 grids.
		cmd.grids = new String[]{ "*_1_ch*", "*_seg"};
		cmd.call();
	}
}