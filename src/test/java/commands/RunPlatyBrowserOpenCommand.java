package commands;

import de.embl.cba.platynereis.platybrowser.PlatyBrowserOpenCommand;
import net.imagej.ImageJ;

public class RunPlatyBrowserOpenCommand
{
	public static void main(final String... args)
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		ij.command().run( PlatyBrowserOpenCommand.class, true );
	}
}
