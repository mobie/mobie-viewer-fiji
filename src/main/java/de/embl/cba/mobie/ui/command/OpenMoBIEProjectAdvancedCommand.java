package de.embl.cba.mobie.ui.command;

import de.embl.cba.mobie.ui.viewer.MoBIEOptions;
import de.embl.cba.mobie.ui.viewer.MoBIEViewer;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>MoBIE>Expert>Open MoBIE Project Expert Mode..." )
public class OpenMoBIEProjectAdvancedCommand implements Command
{
	@Parameter ( label = "Project Location" )
	public String projectLocation = "https://github.com/platybrowser/platybrowser";

	@Parameter ( label = "Project Branch" )
	public String projectBranch = "master";

	@Parameter ( label = "Image Data Storage Type", choices = { "S3", "FileSystem" } )
	public String imageDataStorageType = "S3";

	@Parameter ( label = "Image Data Root Path (only for FileSystem storage)" )
	public String imageDataRootPath = "";

	@Parameter ( label = "Table Data Location" )
	public String tableDataLocation = "https://github.com/platybrowser/platybrowser";

	@Parameter ( label = "Table Data Branch (only for GitHub locations)" )
	public String tableDataBranch = "master";

	@Override
	public void run()
	{
		final MoBIEViewer moBIEViewer = new MoBIEViewer(
				projectLocation,
				MoBIEOptions.options()
						.gitBranch( projectBranch )
						.imageDataStorageType( MoBIEOptions.ImageDataStorageType.valueOf( imageDataStorageType ) )
						.imageDataRootPath( imageDataRootPath )
						.tableDataLocation( tableDataLocation )
						.tableDataBranch( tableDataBranch ) );
	}

	public static void main(final String... args)
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final MoBIEViewer moBIEViewer = new MoBIEViewer(
				"https://github.com/mobie/covid-tomo-datasets",
				MoBIEOptions.options().gitBranch( "norm-bookmarks" ) );
	}
}
