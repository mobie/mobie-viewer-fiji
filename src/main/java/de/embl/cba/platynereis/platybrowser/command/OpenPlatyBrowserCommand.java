package de.embl.cba.platynereis.platybrowser.command;

import de.embl.cba.platynereis.dataset.DatasetsParser;
import de.embl.cba.platynereis.platybrowser.MoBIEViewer;
import de.embl.cba.platynereis.utils.ui.DatasetsDialog;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;


@Plugin(type = Command.class, menuPath = "Plugins>MMB>PlatyBrowser")
public class OpenPlatyBrowserCommand implements Command
{
	public static final String remoteGitLocation = "https://raw.githubusercontent.com/platybrowser/platybrowser-backend/master/data";

	@Override
	public void run()
	{
		final MoBIEViewer moBIEViewer = new MoBIEViewer(
				remoteGitLocation,
				remoteGitLocation );

	}
}
