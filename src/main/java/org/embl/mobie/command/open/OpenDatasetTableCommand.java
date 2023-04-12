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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_OPEN + "Open Dataset Table..." )
public class OpenDatasetTableCommand implements Command {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter( label = "Table Path", required = true )
	public File table;

	@Parameter( label = "Root Folder", required = false )
	public File rootFolder;

	@Parameter( label = "Image Columns (Comma Separated)", required = true )
	public String imageColumns;

	@Parameter( label = "Labels Columns (Comma Separated)", required = false )
	public String labelsColumns;

	@Parameter( label = "Remove Spatial Calibration", required = false )
	public Boolean removeSpatialCalibration = false;

	@Override
	public void run()
	{
		final GridType gridType = GridType.Stitched; // TODO: fetch from UI

		final MoBIESettings settings = new MoBIESettings();
		settings.removeSpatialCalibration( removeSpatialCalibration );

		List< String > images = new ArrayList<>();
		if ( imageColumns != null )
		{
			images = Arrays.asList( imageColumns.split( "," ) );
			images = images.stream().map( s -> s.trim() ).collect( Collectors.toList() );
		}

		List< String > labels = new ArrayList<>();
		if ( labelsColumns != null )
		{
			labels = Arrays.asList( labelsColumns.split( "," ) );
			labels = labels.stream().map( s -> s.trim() ).collect( Collectors.toList() );
		}

		try
		{
			new MoBIE( table.getAbsolutePath(), images, labels, rootFolder.getAbsolutePath(), gridType, settings );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
}