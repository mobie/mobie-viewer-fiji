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

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_OPEN + "Open Multiple Images and Labels..." )
public class OpenMultipleImagesAndLabelsCommand implements Command {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter( label = "Image Path", required = false )
	public File image0;

	@Parameter( label = "Image Path", required = false )
	public File image1;

	@Parameter( label = "Image Path", required = false )
	public File image2;

	@Parameter( label = "Image Path", required = false )
	public File image3;

	@Parameter( label = "Labels Path", required = false )
	public File labels0;

	@Parameter( label = "Labels Table Path", required = false )
	public File table0;

	@Parameter( label = "Labels Path", required = false )
	public File labels1;

	@Parameter( label = "Labels Table Path", required = false )
	public File table1;

	@Parameter( label = "Remove Spatial Calibration", required = false )
	public Boolean removeSpatialCalibration = false;

	@Override
	public void run()
	{
		final GridType gridType = GridType.Stitched; // TODO: fetch from UI

		final ArrayList< String > imageList = new ArrayList<>();
		if ( image0 != null ) imageList.add( image0.getAbsolutePath() );
		if ( image1 != null ) imageList.add( image1.getAbsolutePath() );
		if ( image2 != null ) imageList.add( image2.getAbsolutePath() );
		if ( image3 != null ) imageList.add( image3.getAbsolutePath() );

		final ArrayList< String > labelsList = new ArrayList<>();
		if ( labels0 != null ) labelsList.add( labels0.getAbsolutePath() );
		if ( labels1 != null ) labelsList.add( labels1.getAbsolutePath() );

		final ArrayList< String > tablesList = new ArrayList<>();
		if ( table0 != null ) labelsList.add( table0.getAbsolutePath() );
		if ( table1 != null ) labelsList.add( table1.getAbsolutePath() );

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