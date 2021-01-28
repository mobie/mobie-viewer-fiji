package de.embl.cba.mobie.ui.command;

import de.embl.cba.mobie.ui.viewer.MoBIEOptions;
import de.embl.cba.mobie.ui.viewer.MoBIEViewer;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

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
		new MoBIEViewer(
				projectLocation,
				MoBIEOptions.options()
						.gitProjectBranch( projectBranch )
						.imageDataStorageModality( MoBIEOptions.ImageDataStorageModality.valueOf( imageDataStorageModality ) )
						.imageDataLocation( imageDataLocation )
						.tableDataLocation( tableDataLocation )
						.gitTablesBranch( tableDataBranch ) );
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

//		new MoBIEViewer(
//				"https://github.com/mobie/platybrowser-datasets",
//				MoBIEOptions.options()
//						.gitProjectBranch( "master" )
//						.imageDataStorageModality( MoBIEOptions.ImageDataStorageModality.FileSystem )
//						.imageDataLocation( "/g/arendt/EM_6dpf_segmentation/platy-browser-data" )
//						.tableDataLocation( "https://github.com/vzinche/platybrowser-backend" )
//						.gitTablesBranch( "bookmarks" ) );

		/**
		 * Project location (github or local): "https://github.com/mobie/platybrowser-datasets"
		 * Project branch (for github): "xray"
		 * Load image data from: choice: local
		 * Image data root path (for local): "/g/arendt/EM_6dpf_segmentation/platy-browser-data"
		 * Table data location (github or local): "https://github.com/mobie/platy-browser-data"
		 * Table data branch (for github): "xray"
		 */

		new MoBIEViewer(
				"https://github.com/mobie/platybrowser-datasets",
				MoBIEOptions.options()
						.gitProjectBranch( "xray" )
						.imageDataStorageModality( MoBIEOptions.ImageDataStorageModality.FileSystem )
						.imageDataLocation( "/g/arendt/EM_6dpf_segmentation/platy-browser-data" )
						.tableDataLocation( "https://github.com/mobie/platy-browser-data" )
						.gitTablesBranch( "xray" ) );



	}
}
