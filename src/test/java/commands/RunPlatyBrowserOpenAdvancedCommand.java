package commands;

import de.embl.cba.platynereis.platybrowser.command.PlatyBrowserOpenAdvancedCommand;
import net.imagej.ImageJ;

public class RunPlatyBrowserOpenAdvancedCommand
{
	public static void main(final String... args)
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		ij.command().run( PlatyBrowserOpenAdvancedCommand.class, true );
	}
}
