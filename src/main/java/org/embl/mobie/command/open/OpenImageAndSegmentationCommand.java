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
import java.util.ArrayList;
import java.util.Collections;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_OPEN + "Open Image and Segmentation..." )
public class OpenImageAndSegmentationCommand implements Command {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter( label = "Image Path Regex", required = false )
	public File image;

	@Parameter( label = "Label Mask Path Regex", required = false )
	public File labels;

//	@Parameter( label = "Label Mask Table Path Regex", required = false )
//	public File table;

	// TODO: link to documentation! explain the wildcards are OK, and that
	//   certain fields are optional

	@Override
	public void run()
	{
		final GridType gridType = GridType.Merged; // TODO: fetch from UI

		final ArrayList< String > imageList = new ArrayList<>();
		if ( image != null ) imageList.add( image.getAbsolutePath() );

		final ArrayList< String > labelsList = new ArrayList<>();
		if ( labels != null ) labelsList.add( labels.getAbsolutePath() );

		try
		{
			new MoBIE( Data.Files, imageList, labelsList, null, gridType );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
}