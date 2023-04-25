package projects.lautaro;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenImageAndLabelsCommand;
import org.embl.mobie.command.open.OpenImagesAndLabelsCommand;

import java.io.File;

class LautaroCheckIlastikTracking
{
	public static void main( String[] args ) throws Exception
	{
		new ImageJ().ui().showUI();
		final OpenImageAndLabelsCommand command = new OpenImageAndLabelsCommand();
		command.image = new File("/Volumes/crocker/Lautaro/Drugs_screen/Drosophila_data/analysis/A0001DF000000039/.*--mov.tif=raw");
		command.labels = new File("/Volumes/crocker/Lautaro/Drugs_screen/Drosophila_data/analysis/A0001DF000000039/.*--tracking-oids.h5=labels");
		command.run();
	}
}