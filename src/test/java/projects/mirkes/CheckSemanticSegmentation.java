package projects.mirkes;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenImagesAndSegmentationsCommand;

import java.io.File;

class CheckSemanticSegmentation
{
	public static void main( String[] args ) throws Exception
	{
		new ImageJ().ui().showUI();
		final OpenImagesAndSegmentationsCommand command = new OpenImagesAndSegmentationsCommand();
		command.image0 = new File("/Volumes/cba/exchange/kristina-mirkes/data-test/processed/.*--pro.tif");
		command.image1 = new File("/Volumes/cba/exchange/kristina-mirkes/data-test/processed/.*--pro_prob_worm.tif");
		command.run();
	}
}