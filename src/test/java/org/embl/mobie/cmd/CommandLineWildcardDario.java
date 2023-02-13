package org.embl.mobie.cmd;

class CommandLineWildcardDario
{
	public static final String DIR = "/Volumes/cba/exchange/ARIF/Dario/output/";

	public static void main( String[] args ) throws Exception
	{
		final MoBIECmd cmd = new MoBIECmd();
		//cmd.images = new String[]{ DIR + "*-pro.tif" };
		cmd.segmentations = new String[]{ DIR + "20221004_ibidi_Beas2B_series_3-25xy_cells_preprocess_cp_masks_postprocess.tif" };
		cmd.tables = new String[]{ DIR + "20221004_ibidi_Beas2B_series_3-25xy_cells_preprocess_cp_masks_postprocess.csv" };
		cmd.call();
	}
}