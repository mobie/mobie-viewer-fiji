package develop;

import org.embl.mobie.cmd.TableCmd;

class OpenCellProfilerObjectTableCmd
{
	public static void main( String[] args ) throws Exception
	{
		final TableCmd cmd = new TableCmd();
		cmd.root = "/Users/tischer/Desktop/Agata/analysed";
		cmd.table = "/Users/tischer/Desktop/Agata/analysed/Nuclei.txt";
		cmd.images = new String[]{"Image_FileName_DNA=DAPI;0","Image_FileName_DNA=RPAC1;1"};
		cmd.labels = new String[]{"Image_FileName_CytoplasmLabels=CytoSeg"};
		cmd.removeSpatialCalibration = true;
		cmd.call();
	}
}