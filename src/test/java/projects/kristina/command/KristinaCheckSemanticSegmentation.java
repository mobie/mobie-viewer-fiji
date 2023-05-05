package projects.kristina.command;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenMultipleImagesAndLabelsCommand;

import java.io.File;

class KristinaCheckSemanticSegmentation
{
	public static void main( String[] args ) throws Exception
	{
		new ImageJ().ui().showUI();
		final OpenMultipleImagesAndLabelsCommand command = new OpenMultipleImagesAndLabelsCommand();
		command.image0 = new File("/Volumes/cba/exchange/kristina-mirkes/data/processed/.*--pro.tif");
		command.image1 = new File("/Volumes/cba/exchange/kristina-mirkes/data/processed/.*--pro_prob_worm.tif");
		command.run();
	}
}