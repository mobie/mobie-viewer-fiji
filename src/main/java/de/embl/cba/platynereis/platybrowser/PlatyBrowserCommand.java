package de.embl.cba.platynereis.platybrowser;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;


@Plugin(type = Command.class, menuPath = "Plugins>EMBL>Explore>Platynereis Atlas" )
public class PlatyBrowserCommand implements Command
{
	@Parameter ( label = "Image Data Location", style = "directory" )
	public File imagesLocation = new File("/Volumes/arendt/EM_6dpf_segmentation/platy-browser-data/data");

	@Parameter ( label = "Table Data Location" )
	public String tablesLocation = "https://git.embl.de/tischer/platy-browser-tables/raw/dev/data";

	@Parameter ( label = "Version", choices = { "0.0.0", "0.0.1", "0.1.0", "0.1.1", "0.2.0", "0.2.1" } )
	public String version;

	@Override
	public void run()
	{
		new PlatyBrowser(
				version,
				imagesLocation.toString(),
				tablesLocation );
	}
}
