package de.embl.cba.platynereis.platybrowser;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;


@Plugin(type = Command.class, menuPath = "Plugins>EMBL>Explore>Platynereis Atlas (Remote)" )
public class PlatyBrowserRemoteCommand implements Command
{
	@Override
	public void run()
	{
		new PlatyBrowser( "http://10.11.4.195:8000",
				"https://git.embl.de/tischer/platy-browser-tables/raw/master/" );
	}
}
