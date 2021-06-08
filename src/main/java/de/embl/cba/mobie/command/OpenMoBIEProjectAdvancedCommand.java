package de.embl.cba.mobie.command;

import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.MoBIESettings;
import de.embl.cba.mobie.source.ImageDataFormat;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;

@Plugin(type = Command.class, menuPath = "Plugins>MoBIE>Open>Advanced>Open MoBIE Project Expert Mode..." )
public class OpenMoBIEProjectAdvancedCommand implements Command
{
	@Parameter ( label = "Project Location" )
	public String projectLocation = "https://github.com/platybrowser/platybrowser";

	@Parameter ( label = "Project Branch" )
	public String projectBranch = "master";

	@Parameter ( label = "Image Data Storage Modality", choices = { "S3", "FileSystem" } )
	public String imageDataStorageModality = "S3";

	@Parameter ( label = "Image Data Location" )
	public String imageDataLocation = "https://github.com/platybrowser/platybrowser";

	@Parameter ( label = "Table Data Location" )
	public String tableDataLocation = "https://github.com/platybrowser/platybrowser";

	@Parameter ( label = "Table Data Branch" )
	public String tableDataBranch = "master";

	@Override
	public void run()
	{
		try
		{
			new MoBIE(
					projectLocation,
					MoBIESettings.settings()
							.gitProjectBranch( projectBranch )
							.imageDataFormat( ImageDataFormat.valueOf( imageDataStorageModality ) )
							.imageDataLocation( imageDataLocation )
							.tableDataLocation( tableDataLocation )
							.gitTablesBranch( tableDataBranch ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	public static void main(final String... args)
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();


		/**
		 * Project location (github or local): "https://github.com/mobie/platybrowser-datasets"
		 * Project branch (for github): "master"
		 * Load image data from: choice: local
		 * Image data root path (for local): "/g/arendt/EM_6dpf_segmentation/platy-browser-data"
		 * Table data location (github or local): "https://github.com/vzinche/platybrowser-backend"
		 * Table data branch (for github):"bookmarks"
		 */

		// try {
		// 	new MoBIE(
		// 			"https://github.com/mobie/platybrowser-datasets",
		// 			MoBIESettings.settings()
		// 					.gitProjectBranch( "master" )
		// 					.imageDataStorageModality( MoBIESettings.ImageDataStorageModality.FileSystem )
		// 					.imageDataLocation( "/g/arendt/EM_6dpf_segmentation/platy-browser-data" )
		// 					.tableDataLocation( "https://github.com/vzinche/platybrowser-backend" )
		// 					.gitTablesBranch( "bookmarks" ) );
		// } catch (IOException e) {
		// 	e.printStackTrace();
		// }

		/**
		 * Project location (github or local): "https://github.com/mobie/platybrowser-datasets"
		 * Project branch (for github): "xray"
		 * Load image data from: choice: local
		 * Image data root path (for local): "/g/arendt/EM_6dpf_segmentation/platy-browser-data"
		 * Table data location (github or local): "https://github.com/mobie/platy-browser-data"
		 * Table data branch (for github): "xray"
		 */

		try {
			new MoBIE(
					"https://github.com/mobie/platybrowser-datasets",
					MoBIESettings.settings()
							.gitProjectBranch( "xray" )
							.imageDataFormat( ImageDataFormat.BdvN5 )
							.imageDataLocation( "/g/arendt/EM_6dpf_segmentation/platy-browser-data" )
							.tableDataLocation( "https://github.com/mobie/platy-browser-data" )
							.gitTablesBranch( "xray" ) );
		} catch (IOException e) {
			e.printStackTrace();
		}


	}
}
