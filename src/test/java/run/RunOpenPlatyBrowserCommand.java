package run;

import de.embl.cba.mobie.command.OpenPlatyBrowserCommand;
import net.imagej.ImageJ;

public class RunOpenPlatyBrowserCommand
{
	public static void main(final String... args)
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		ij.command().run( OpenPlatyBrowserCommand.class, true );
	}
}
