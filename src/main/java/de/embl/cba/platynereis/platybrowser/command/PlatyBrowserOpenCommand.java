package de.embl.cba.platynereis.platybrowser.command;

import de.embl.cba.platynereis.platybrowser.PlatyBrowser;
import de.embl.cba.platynereis.utils.ui.VersionsDialog;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;


@Plugin(type = Command.class, menuPath = "Plugins>PlatyBrowser>Open PlatyBrowser" )
public class PlatyBrowserOpenCommand implements Command
{
	// TODO: Make this a SciJava setting, look how they did it in ilastik...
	public static final String remoteGitLocation = "https://raw.githubusercontent.com/platybrowser/platybrowser-backend/master/data";

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
