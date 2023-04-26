package org.embl.mobie.command.open;

import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.lib.transform.GridType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_OPEN + "Open Image and Label Mask..." )
public class OpenImageAndLabelMaskCommand implements Command {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter( label = "Image Path", required = false )
	public File image;

	@Parameter( label = "Label Mask Path", required = false )
	public File labels;

	@Parameter( label = "Label Mask Table Path", required = false )
	public File table;

	@Parameter( label = "Remove Spatial Calibration", required = false )
	public Boolean removeSpatialCalibration = false;

	@Override
	public void run()
	{
		final GridType gridType = GridType.Stitched; // TODO: fetch from UI

		final ArrayList< String > imageList = new ArrayList<>();
		if ( image != null ) imageList.add( image.getAbsolutePath() );

		final ArrayList< String > labelsList = new ArrayList<>();
		if ( labels != null ) labelsList.add( labels.getAbsolutePath() );

		final ArrayList< String > tablesList = new ArrayList<>();
		if ( table != null ) tablesList.add( table.getAbsolutePath() );

		final MoBIESettings settings = new MoBIESettings();
		settings.removeSpatialCalibration( removeSpatialCalibration );

		try
		{
			new MoBIE( imageList, labelsList, tablesList, null, gridType, settings );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
}