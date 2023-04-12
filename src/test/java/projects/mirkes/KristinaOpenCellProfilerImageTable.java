package projects.mirkes;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenImageTableCommand;

import java.io.File;

class KristinaOpenCellProfilerImageTable
{
	public static void main( String[] args ) throws Exception
	{
		new ImageJ().ui().showUI();
		final OpenImageTableCommand command = new OpenImageTableCommand();
		command.rootFolder = new File("/Volumes/cba/exchange/kristina-mirkes/data/processed");
		command.table = new File("/Volumes/cba/exchange/kristina-mirkes/data/processed/Image.txt");
		command.imageColumns = "RawData=FileName_RawData,WormProb=FileName_WormProb";
		//command.labelsColumns = "Worm=FileName_Worm???";
		command.removeSpatialCalibration = true;
		command.run();
	}
}