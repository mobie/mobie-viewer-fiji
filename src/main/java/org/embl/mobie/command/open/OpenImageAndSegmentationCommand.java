package org.embl.mobie.command.open;

import ij.IJ;
import mpicbg.spim.data.SpimDataException;
import org.embl.mobie.MoBIE;
import org.embl.mobie.command.CommandConstants;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

import java.io.File;
import java.io.IOException;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_OPEN + "Open Image and Segmentation..." )
public class OpenImageAndSegmentationCommand implements Command {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter( label = "Image Path", required = false )
	public File image;

	@Parameter( label = "Label Mask Path", required = false )
	public File segmentation;

	@Parameter( label = "Label Mask Table Path", required = false )
	public File table;

	// TODO: link to documentation! explain the wildcards are OK, and that
	//   certain fields are optional
	//@Parameter( label = "Help", required = false, callback = "help" )
	//public Button button = null;

	//@Parameter( label = "Auto Pair Images and Segmentations" )
	//public Boolean autoPair = true;

	@Override
	public void run()
	{
		if ( image == null && segmentation == null )
		{
			IJ.showMessage( "Please either provide a path to images or segmentations." );
			return;
		}

		try
		{
			new MoBIE( "ImageJ UI", image, segmentation, table );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
}