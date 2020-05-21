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
		final ArrayList< String > datasets = new DatasetsParser().datasetsFromDataSource( remoteGitLocation );

		final String defaultDataset = datasets.get( datasets.size() - 1 );

		final MoBIEViewer moBIEViewer = new MoBIEViewer(
				defaultDataset,
				datasets,
				remoteGitLocation,
				remoteGitLocation );

	}
}
