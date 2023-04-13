package projects.kristina.command;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenDatasetTableCommand;

import java.io.File;

class KristinaOpenCellProfilerImageTable
{
	public static void main( String[] args ) throws Exception
	{
		new ImageJ().ui().showUI();
		final OpenDatasetTableCommand command = new OpenDatasetTableCommand();
		command.rootFolder = new File("/Volumes/cba/exchange/kristina-mirkes/data/processed");
		command.table = new File("/Volumes/cba/exchange/kristina-mirkes/data/processed/Image.txt");
		command.images = "RawData=FileName_RawData,WormProb=FileName_WormProb";
		//command.labelsColumns = "Worm=FileName_Worm???";
		command.removeSpatialCalibration = true;
		command.run();
	}
}