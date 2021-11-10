package tests;

import net.imagej.ImageJ;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;
import org.embl.mobie.viewer.source.ImageDataFormat;

import java.io.IOException;

public class TestOpenAdvancedProject
{
	// TODO: Make a proper test from this code
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
		} catch ( IOException e) {
			e.printStackTrace();
		}
	}
}
