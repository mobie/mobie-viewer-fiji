package org.embl.mobie.viewer.command;

import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;
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

	@Parameter ( label = "Image Data Storage Modality", choices = { ImageDataFormat.BDVN5, ImageDataFormat.BDVN5S3, ImageDataFormat.BDVOMEZARR, ImageDataFormat.BDVOMEZARRS3, ImageDataFormat.OMEZARR, ImageDataFormat.OMEZARRS3, ImageDataFormat.OPENORGANELLES3 } )
	public String imageDataStorageModality = ImageDataFormat.OMEZARRS3;

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


}
