package projects.mirkes.cmd;

import net.imagej.ImageJ;
import org.embl.mobie.cmd.FilesCmd;

import java.io.File;

class KristinaCheckSemanticSegmentationCmd
{
	public static void main( String[] args ) throws Exception
	{
		new ImageJ().ui().showUI();
		final FilesCmd cmd = new FilesCmd();
		cmd.images = new String[]{
				"/Volumes/cba/exchange/kristina-mirkes/data-test/processed/.*--pro.tif",
				"/Volumes/cba/exchange/kristina-mirkes/data-test/processed/.*--pro_prob_worm.tif" } ;
		cmd.removeSpatialCalibration = true;
		cmd.call();
	}
}