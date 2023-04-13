package projects.kristina.cmd;

import net.imagej.ImageJ;
import org.embl.mobie.cmd.FilesCmd;

class KristinaCheckIlastikSegmentationCmd
{
	public static void main( String[] args ) throws Exception
	{
		final FilesCmd cmd = new FilesCmd();
		cmd.root = "/Volumes/cba/exchange/kristina-mirkes/data/processed";
		cmd.images = new String[]{
				".*--pro.tif",
				".*--pro_prob_worm.tif" } ;
		cmd.removeSpatialCalibration = true;
		cmd.call();
	}
}