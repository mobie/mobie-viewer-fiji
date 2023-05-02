package develop;

import org.embl.mobie.cmd.TableCmd;

class OpenCellProfilerImageTableCmd
{
	public static void main( String[] args ) throws Exception
	{
		final TableCmd cmd = new TableCmd();
		cmd.root = "/Users/tischer/Desktop/Agata/analysed";
		cmd.table = "/Users/tischer/Desktop/Agata/analysed/Image.txt";
		cmd.images = new String[]{"DAPI=FileName_DNA;0","RPAC1=FileName_DNA;1"};
		cmd.labels = new String[]{"CytoSeg=FileName_CytoplasmLabels","NucleiSeg=ObjectsFileName_Nuclei"};
		cmd.removeSpatialCalibration = true;
		cmd.call();
	}
}