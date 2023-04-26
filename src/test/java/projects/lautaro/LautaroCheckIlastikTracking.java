package projects.lautaro;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenImageAndLabelMaskCommand;

import java.io.File;

class LautaroCheckIlastikTracking
{
	public static void main( String[] args ) throws Exception
	{
		new ImageJ().ui().showUI();
		final OpenImageAndLabelMaskCommand command = new OpenImageAndLabelMaskCommand();
		command.image = new File("/Volumes/crocker/Lautaro/Drugs_screen/Drosophila_data/analysis/A0001DF000000039/.*--mov.tif=raw");
		command.labels = new File("/Volumes/crocker/Lautaro/Drugs_screen/Drosophila_data/analysis/A0001DF000000039/.*--tracking-oids.h5=labels");
		command.table = new File("/Volumes/crocker/Lautaro/Drugs_screen/Drosophila_data/analysis/A0001DF000000039/.*--tracking-table.csv");
		command.run();
	}
}