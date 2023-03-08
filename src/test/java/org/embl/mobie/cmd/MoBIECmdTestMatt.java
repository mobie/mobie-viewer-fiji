package org.embl.mobie.cmd;

import static org.junit.jupiter.api.Assertions.*;

class MoBIECmdTestMatt
{
	public static void main( String[] args ) throws Exception
	{
		final MoBIECmd cmd = new MoBIECmd();
		cmd.images = new String[]{ "/Users/tischer/Desktop/matt/preprocessed/*.tif" };
		cmd.segmentations = new String[]{ "/Users/tischer/Desktop/matt/analysed/*_seg.tif" };
		cmd.tables = new String[]{ "/Users/tischer/Desktop/matt/analysed/*_seg.csv" };
		cmd.grids = new String[]{ "*_1_2*"};
		cmd.call();
	}
}