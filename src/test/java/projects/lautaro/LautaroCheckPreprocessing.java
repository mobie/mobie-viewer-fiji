package projects.lautaro;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenMultipleImagesAndLabelsCommand;

import java.io.File;

class LautaroCheckPreprocessing
{
	public static void main( String[] args ) throws Exception
	{
		new ImageJ().ui().showUI();
		final OpenMultipleImagesAndLabelsCommand command = new OpenMultipleImagesAndLabelsCommand();
		command.image0 = new File("/Volumes/crocker/Lautaro/Drugs_screen/Drosophila_data/analysis/A0001DF000000031/.*--mov.tif");
		command.image1 = new File("/Volumes/crocker/Lautaro/Drugs_screen/Drosophila_data/analysis/A0001DF000000031/.*--raw.tif");
		command.run();
	}
}