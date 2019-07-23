package commands;

import de.embl.cba.platynereis.platybrowser.PlatyBrowserLocalCommand;
import net.imagej.ImageJ;

public class RunPlatyBrowserLocalCommand
{
	public static void main(final String... args)
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		ij.command().run( PlatyBrowserLocalCommand.class, true );
	}
}
