package projects.agata.cmd;

import org.embl.mobie.cmd.TableCmd;

class AgataOpenCellProfilerImageTableCmd
{
	public static void main( String[] args ) throws Exception
	{
		final TableCmd cmd = new TableCmd();
		cmd.root = "/g/cba/exchange/agata-misiaszek/data/analysed";
		cmd.table = "/g/cba/exchange/agata-misiaszek/data/analysed/Image.txt";
		cmd.images = new String[]{"DAPI=FileName_DNA;0","RPAC1=FileName_DNA;1"};
		cmd.labels = new String[]{"Cytoplasm=FileName_CytoplasmLabels","Nucleoplasm=FileName_NucleoplasmLabels","Nucleoli=FileName_NucleoliLabels"};
		cmd.removeSpatialCalibration = true;
		cmd.call();
	}
}