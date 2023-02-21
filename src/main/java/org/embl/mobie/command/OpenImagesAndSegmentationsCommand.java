package org.embl.mobie.command;

import ij.IJ;
import mpicbg.spim.data.SpimDataException;
import org.embl.mobie.MoBIE;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_ROOT + "Open>Open Images and Segmentations..." )
public class OpenImagesAndSegmentationsCommand implements Command {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter( label = "Image Path" )
	public String image = null;

	@Parameter( label = "Label Mask Path" )
	public String segmentation = null;

	@Parameter( label = "Label Mask Feature Table Path" )
	public String table = null;

	@Parameter( label = "Auto Pair Images and Segmentations" )
	public Boolean autoPair = true;

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
			new MoBIE( "", new String[]{ image }, new String[]{ segmentation }, new String[]{ table }, autoPair );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
}