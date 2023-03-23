package projects.mirkes;

import org.embl.mobie.cmd.MoBIECmd;
import org.embl.mobie.command.open.OpenImageAndSegmentationCommand;

import java.io.File;

class CheckSemanticSegmentation
{
	public static void main( String[] args ) throws Exception
	{
		final OpenImageAndSegmentationCommand command = new OpenImageAndSegmentationCommand();
		command.image = new File("/Users/tischer/Desktop/Kristina/data/processed-test/rnai_exp--*--pro.tif");
		command.labels = new File("/Users/tischer/Desktop/Kristina/data/processed-test/rnai_exp--*--pro_semseg.tif");
	}
}