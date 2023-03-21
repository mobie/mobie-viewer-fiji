package org.embl.mobie.command.open;

import ij.IJ;
import org.embl.mobie.Data;
import org.embl.mobie.MoBIE;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.lib.transform.GridType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_OPEN + "Open Image and Segmentation..." )
public class OpenImageAndSegmentationCommand implements Command {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter( label = "Image Path", required = false )
	public File image;

	@Parameter( label = "Label Mask Path", required = false )
	public File labels;

	@Parameter( label = "Label Mask Table Path", required = false )
	public File table;

	// TODO: link to documentation! explain the wildcards are OK, and that
	//   certain fields are optional

	@Override
	public void run()
	{
		if ( image == null && labels == null )
		{
			IJ.showMessage( "Please either provide a path to images or labels." );
			return;
		}

		try
		{
			new MoBIE( Data.Files, new String[]{ image.getAbsolutePath() }, new String[]{  labels.getAbsolutePath() }, null, GridType.None );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
}