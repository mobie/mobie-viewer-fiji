package run;

import net.imagej.ImageJ;

public class RunImageJ
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
	}
}
