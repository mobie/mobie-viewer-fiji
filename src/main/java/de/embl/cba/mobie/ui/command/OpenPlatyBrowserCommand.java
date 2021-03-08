package de.embl.cba.mobie.ui.command;

import de.embl.cba.mobie.ui.MoBIE;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;


@Plugin(type = Command.class, menuPath = "Plugins>MoBIE>Open>Open PlatyBrowser")
public class OpenPlatyBrowserCommand implements Command
{
	public static final String githubProject = "https://github.com/platybrowser/platybrowser";

	@Override
	public void run()
	{
		new MoBIE( githubProject );
	}
}
