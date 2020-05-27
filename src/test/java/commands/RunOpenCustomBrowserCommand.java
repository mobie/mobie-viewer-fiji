package commands;

import net.imagej.ImageJ;

public class RunOpenCustomBrowserCommand
{
	public static void main(final String... args)
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		ij.command().run( de.embl.cba.mobie.command.OpenCustomBrowserCommand.class, true );
	}
}
