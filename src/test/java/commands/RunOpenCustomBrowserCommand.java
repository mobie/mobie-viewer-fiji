package commands;

import de.embl.cba.platynereis.platybrowser.command.OpenCustomBrowserCommand;
import net.imagej.ImageJ;

public class RunOpenCustomBrowserCommand
{
	public static void main(final String... args)
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		ij.command().run( de.embl.cba.platynereis.platybrowser.command.OpenCustomBrowserCommand.class, true );
	}
}
