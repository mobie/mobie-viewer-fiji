package org.embl.mobie.cmd;

import static org.junit.jupiter.api.Assertions.*;

class MoBIECmdTest00
{
	public static void main( String[] args ) throws Exception
	{
		final MoBIECmd cmd = new MoBIECmd();
		cmd.images = new String[]{ "/Users/tischer/Desktop/matt/preprocessed/*.tif" };
		cmd.segmentations = new String[]{ "/Users/tischer/Desktop/matt/analysed/*.tif" };
		cmd.tables = null;
		cmd.call();
	}
}