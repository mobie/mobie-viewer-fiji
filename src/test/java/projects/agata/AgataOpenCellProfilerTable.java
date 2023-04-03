package projects.agata;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenImageTableCommand;
import org.embl.mobie.command.open.OpenImagesAndSegmentationCommand;

import java.io.File;

class AgataOpenCellProfilerTable
{
	public static void main( String[] args ) throws Exception
	{
		new ImageJ().ui().showUI();
		final OpenImageTableCommand command = new OpenImageTableCommand();
		command.table = new File("/Volumes/cba/exchange/agata-misiaszek/data/analysed/Image.txt");
		command.rootFolder = new File("/Volumes/cba/exchange/agata-misiaszek/data/analysed");
		command.imageColumns = "FileName_DNA";
		command.labelsColumns = "FileName_CytoplasmLabels,ObjectsFileName_Nuclei";
		command.run();
	}
}