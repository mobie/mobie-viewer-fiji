package run;

import org.embl.mobie.viewer.command.OpenMoBIEProjectCommand;
import net.imagej.ImageJ;

public class RunOpenCustomBrowserCommand
{
	public static void main(final String... args)
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		ij.command().run( OpenMoBIEProjectCommand.class, true );
	}
}
