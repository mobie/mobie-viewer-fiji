package de.embl.cba.platynereis.platybrowser.command;

import de.embl.cba.platynereis.platybrowser.MoBIEViewer;
import de.embl.cba.platynereis.utils.ui.VersionsDialog;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;


@Plugin(type = Command.class, menuPath = "Plugins>MMB>PlatyBrowser")
public class OpenPlatyBrowserCommand implements Command
{
	public static final String remoteGitLocation = "https://raw.githubusercontent.com/platybrowser/platybrowser-backend/master/data";

	@Override
	public void run()
	{
		final String version = new VersionsDialog().showDialog( remoteGitLocation + "/versions.json" );
		if ( version == null ) return;

		new MoBIEViewer(
				version,
				remoteGitLocation,
				remoteGitLocation );
	}
}
