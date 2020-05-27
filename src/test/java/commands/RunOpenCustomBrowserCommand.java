package commands;

import de.embl.cba.mobie.command.OpenMoBIEViewerCommand;
import net.imagej.ImageJ;

public class RunOpenCustomBrowserCommand
{
	public static void main(final String... args)
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		ij.command().run( OpenMoBIEViewerCommand.class, true );
	}
}
