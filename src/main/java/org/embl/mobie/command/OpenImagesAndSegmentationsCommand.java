package org.embl.mobie.command;

import ij.IJ;
import mpicbg.spim.data.SpimDataException;
import org.embl.mobie.lib.MoBIE;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import picocli.CommandLine.Option;

import java.io.IOException;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_ROOT + "Open>Open Images and Segmentations..." )
public class OpenImagesAndSegmentationsCommand implements Command {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter( label = "Image path (use * for multiple)" )
	public String image = null;

	@Parameter( label = "Label mask path (use * for multiple)" )
	public String segmentation = null;

	@Parameter( label = "Label mask feature table path (use * for multiple)" )
	public String table = null;

	@Parameter( label = "Create a grid view" )
	public Boolean grid = true;

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
			new MoBIE( "", new String[]{ image }, new String[]{ segmentation }, new String[]{ table }, grid );
		} catch ( SpimDataException e )
		{
			e.printStackTrace();
		} catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
}