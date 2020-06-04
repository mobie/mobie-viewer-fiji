package de.embl.cba.mobie.command;

import de.embl.cba.mobie.viewer.MoBIEViewer;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;


@Plugin(type = Command.class, menuPath = "Plugins>MoBIE>Open PlatyBrowser")
public class OpenPlatyBrowserCommand implements Command
{
	public static final String githubProject = "https://github.com/platybrowser/platybrowser";

	@Override
	public void run()
	{
		new MoBIEViewer( githubProject );
	}
}
