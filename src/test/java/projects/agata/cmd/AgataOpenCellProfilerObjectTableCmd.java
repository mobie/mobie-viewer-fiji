package projects.agata.cmd;

import org.embl.mobie.cmd.TableCmd;

class AgataOpenCellProfilerObjectTableCmd
{
	public static void main( String[] args ) throws Exception
	{
		final TableCmd cmd = new TableCmd();
		cmd.root = "/g/cba/exchange/agata-misiaszek/data/analysed";
		cmd.table = "/g/cba/exchange/agata-misiaszek/data/analysed/Nuclei.txt";
		cmd.images = new String[]{"DAPI=FileName_DNA;0","RPAC1=FileName_DNA;1"};
		cmd.labels = new String[]{"CytoSeg=FileName_CytoplasmLabels","NucleiSeg=ObjectsFileName_Nuclei"};
		cmd.removeSpatialCalibration = true;
		cmd.call();
	}
}