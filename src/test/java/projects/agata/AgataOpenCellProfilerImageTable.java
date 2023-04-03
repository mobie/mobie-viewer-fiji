package projects.agata;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenImageTableCommand;
import org.embl.mobie.command.open.OpenImagesAndSegmentationCommand;

import java.io.File;

class AgataOpenCellProfilerImageTable
{
	public static void main( String[] args ) throws Exception
	{
		new ImageJ().ui().showUI();
		final OpenImageTableCommand command = new OpenImageTableCommand();
		command.table = new File("/Users/tischer/Desktop/Agata/analysed/Image.txt");
		command.rootFolder = new File("/Users/tischer/Desktop/Agata/analysed");
		command.imageColumns = "RPAC1=FileName_DNA;1";
		command.labelsColumns = "FileName_CytoplasmLabels,ObjectsFileName_Nuclei";
		command.removeSpatialCalibration = true;
		command.run();
	}
}