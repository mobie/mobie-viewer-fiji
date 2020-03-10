package de.embl.cba.platynereis.platybrowser.command;

import de.embl.cba.platynereis.platybrowser.PlatyBrowser;
import de.embl.cba.platynereis.utils.ui.VersionsDialog;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;


@Plugin(type = Command.class, menuPath = "Plugins>MMB>CustomBrowser" )
public class PlatyBrowserOpenAdvancedCommand implements Command
{
	@Parameter ( label = "Image Data Location", style = "directory" )
	public String imagesLocation = "/Volumes/arendt/EM_6dpf_segmentation/platy-browser-data/data";

	@Parameter ( label = "Table Data Location" )
	public String tablesLocation = "/Volumes/arendt/EM_6dpf_segmentation/platy-browser-data/data";
	@Override
	public void run()
	{
		final String version = new VersionsDialog().showDialog( tablesLocation + "/versions.json" );
		if ( version == null ) return;

		new PlatyBrowser(
				version,
				imagesLocation,
				tablesLocation );
	}

	public static void main(final String... args)
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		ij.command().run( PlatyBrowserOpenAdvancedCommand.class, true );
	}
}
