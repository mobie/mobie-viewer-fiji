package projects.agata;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenDatasetTableCommand;

import java.io.File;

class AgataOpenCellProfilerImageTable
{
	public static void main( String[] args ) throws Exception
	{
		new ImageJ().ui().showUI();
		final OpenDatasetTableCommand command = new OpenDatasetTableCommand();
		command.table = new File("/g/cba/exchange/agata-misiaszek/data/analysed/Image.txt");
		command.rootFolder = new File("/g/cba/exchange/agata-misiaszek/data/analysed/");
		command.imageColumns = "DAPI=FileName_DNA;0,RPAC1=FileName_DNA;1";
		command.labelsColumns = "CytoSeg=FileName_CytoplasmLabels,NucleiSeg=ObjectsFileName_Nuclei";
		command.removeSpatialCalibration = true;
		command.run();
	}
}