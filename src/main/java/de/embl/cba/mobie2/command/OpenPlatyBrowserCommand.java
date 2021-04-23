package de.embl.cba.mobie2.command;

import de.embl.cba.mobie.ui.MoBIE;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;


@Plugin(type = Command.class, menuPath = "Plugins>MoBIE2>Open>Open PlatyBrowser")
public class OpenPlatyBrowserCommand implements Command
{
	@Override
	public void run()
	{
		new MoBIE( "https://github.com/mobie/platybrowser-datasets" );
	}
}
