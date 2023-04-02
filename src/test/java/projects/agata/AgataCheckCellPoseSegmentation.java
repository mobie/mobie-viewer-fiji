package projects.agata;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenImagesAndSegmentationCommand;

import java.io.File;

class AgataCheckCellPoseSegmentation
{
	public static void main( String[] args ) throws Exception
	{
		new ImageJ().ui().showUI();
		final OpenImagesAndSegmentationCommand command = new OpenImagesAndSegmentationCommand();
		command.image0 = new File("/Volumes/cba/exchange/agata-misiaszek/data/analysed/.*.ome.tif");
		command.labels = new File("/Volumes/cba/exchange/agata-misiaszek/data/analysed/.*.ome_cp_masks.tif");
		command.run();
	}
}