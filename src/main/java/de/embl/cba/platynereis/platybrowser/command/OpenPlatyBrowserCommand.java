package de.embl.cba.platynereis.platybrowser.command;

import de.embl.cba.platynereis.platybrowser.PlatyBrowser;
import de.embl.cba.platynereis.utils.ui.VersionsDialog;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;


@Plugin(type = Command.class, menuPath = "Plugins>MMB>PlatyBrowser")
public class OpenPlatyBrowserCommand implements Command
{
	public static final String remoteGitLocation = "https://raw.githubusercontent.com/platybrowser/platybrowser-backend/master/data";

	@Override
	public void run()
	{
		final String version = new VersionsDialog().showDialog( remoteGitLocation + "/versions.json" );
		if ( version == null ) return;

		new PlatyBrowser(
				version,
				remoteGitLocation,
				remoteGitLocation );
	}
}
