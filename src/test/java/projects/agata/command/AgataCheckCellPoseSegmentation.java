package projects.agata.command;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenMultipleImagesAndLabelsCommand;

import java.io.File;

class AgataCheckCellPoseSegmentation
{
	public static void main( String[] args ) throws Exception
	{
		new ImageJ().ui().showUI();
		final OpenMultipleImagesAndLabelsCommand command = new OpenMultipleImagesAndLabelsCommand();
		command.image0 = new File("/Volumes/cba/exchange/agata-misiaszek/data/analysed/.*.ome.tif");
		command.labels0 = new File("/Volumes/cba/exchange/agata-misiaszek/data/analysed/.*.ome_cp_masks.tif");
		command.run();
	}
}