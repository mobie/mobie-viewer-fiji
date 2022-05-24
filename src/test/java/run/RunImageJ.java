package run;

import net.imagej.ImageJ;
import org.scijava.command.CommandService;

public class RunImageJ
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new net.imagej.ImageJ();
		imageJ.ui().showUI();
	}
}
