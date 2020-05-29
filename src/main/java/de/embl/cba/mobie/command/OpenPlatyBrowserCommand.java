package de.embl.cba.mobie.command;

import de.embl.cba.mobie.viewer.MoBIEViewer;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;


@Plugin(type = Command.class, menuPath = "Plugins>MoBIE>Open PlatyBrowser")
public class OpenPlatyBrowserCommand implements Command
{
	public static final String remoteGitLocation = "https://github.com/platybrowser/platybrowser";

	@Override
	public void run()
	{
		final MoBIEViewer moBIEViewer = new MoBIEViewer(
				remoteGitLocation,
				remoteGitLocation );

	}
}
