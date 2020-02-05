package de.embl.cba.platynereis.platybrowser;

import de.embl.cba.platynereis.utils.ui.VersionsDialog;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;


@Plugin(type = Command.class, menuPath = "Plugins>EMBL>Platynereis Atlas>Open " )
public class PlatyBrowserOpenCommand implements Command
{
	public static final String remoteGitLocation = "https://git.embl.de/tischer/platy-browser-tables/raw/master/data";

	@Override
	public void run()
	{
		final String version = new VersionsDialog().showDialog( remoteGitLocation + "/versions.json" );

		new PlatyBrowser(
				version,
				remoteGitLocation,
				remoteGitLocation );
	}
}
