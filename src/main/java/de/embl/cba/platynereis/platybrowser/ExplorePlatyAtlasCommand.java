package de.embl.cba.platynereis.platybrowser;

import de.embl.cba.platynereis.PlatynereisImageSourcesModel;
import de.embl.cba.tables.modelview.segments.*;
import de.embl.cba.tables.modelview.views.DefaultTableAndBdvViews;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.*;


@Plugin(type = Command.class, menuPath = "Plugins>EMBL>Explore>Platynereis Atlas" )
public class ExplorePlatyAtlasCommand implements Command
{
	@Parameter ( label = "Platynereis Atlas Data Folder", style = "directory")
	public File dataFolder;

	@Override
	public void run()
	{
		new PlatyBrowserMainFrame( dataFolder );
	}
}
