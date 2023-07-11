package org.embl.mobie.lib;

import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import org.apache.commons.io.FilenameUtils;
import org.embl.mobie.MoBIESettings;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.serialize.DataSource;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.serialize.ImageDataSource;
import org.embl.mobie.lib.serialize.SegmentationDataSource;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.lib.serialize.display.ImageDisplay;
import org.embl.mobie.lib.serialize.display.SegmentationDisplay;
import org.embl.mobie.lib.table.TableDataFormat;
import spimdata.util.Displaysettings;

import java.io.File;
import java.util.Arrays;

public class SpimDataAdder
{
	private final AbstractSpimData< ? > image;
	private final AbstractSpimData< ? > labels;
	private final StorageLocation tableStorageLocation;
	private final TableDataFormat tableDataFormat;
	private Dataset dataset;
	private MoBIESettings settings;

	public SpimDataAdder( AbstractSpimData< ? > image, AbstractSpimData< ? > labels, StorageLocation tableStorageLocation, TableDataFormat tableDataFormat )
	{
		this.image = image;
		this.labels = labels;
		this.tableStorageLocation = tableStorageLocation;
		this.tableDataFormat = tableDataFormat;
	}

	public void addData( Dataset dataset, MoBIESettings settings )
	{
		this.dataset = dataset;
		this.settings = settings;

		addSpimData( image, false );

		if ( labels != null )
			addSpimData( labels, true );
	}

	private void addSpimData( AbstractSpimData< ? > spimData, boolean isSegmentation )
	{
		final ImageDataFormat imageDataFormat = ImageDataFormat.SpimData;

		if ( tableDataFormat != null )
			settings.addTableDataFormat( tableDataFormat );

		final int numSetups = spimData.getSequenceDescription().getViewSetupsOrdered().size();

		for ( int setupIndex = 0; setupIndex < numSetups; setupIndex++ )
		{
			final StorageLocation storageLocation = new StorageLocation();
			storageLocation.data = spimData;
			storageLocation.setChannel( setupIndex );
			final String setupName = spimData.getSequenceDescription().getViewSetupsOrdered().get( setupIndex ).getName();
			String imageName = getImageName( setupName, numSetups, setupIndex );

			DataSource dataSource;
			if ( isSegmentation )
			{
				dataSource = new SegmentationDataSource( imageName, imageDataFormat, storageLocation, tableDataFormat, tableStorageLocation );
				addSegmentationView( spimData, setupIndex, imageName );
			}
			else
			{
				dataSource = new ImageDataSource( imageName, imageDataFormat, storageLocation );
				addImageView( spimData, setupIndex, imageName );
			}

			dataSource.preInit( true );
			dataset.addDataSource( dataSource );
			dataset.is2D( MoBIEHelper.is2D( spimData, setupIndex ) );
		}
	}

	private String getImageName( String setupName, int numImages, int setupIndex )
	{
		String imageName = FilenameUtils.removeExtension( new File( setupName ).getName() );
		if ( numImages == 1)
		{
			imageName = imageName.replaceAll( " channel.*", "" );
		}
		else
		{
			imageName = imageName.replaceAll( " channel ", "ch_" );
		}

		return imageName;
	}

	private void addImageView( AbstractSpimData< ? > spimData, int imageIndex, String imageName )
	{
		final Displaysettings displaysettings = spimData.getSequenceDescription().getViewSetupsOrdered().get( imageIndex ).getAttribute( Displaysettings.class );

		String color = "White";
		double[] contrastLimits = null;

		if ( displaysettings != null )
		{
			// FIXME: Wrong color from Bio-Formats
			//    https://forum.image.sc/t/bio-formats-color-wrong-for-imagej-images/76021/15
			//    https://github.com/BIOP/bigdataviewer-image-loaders/issues/8
			color = "White"; // ColorHelper.getString( displaysettings.color );
			contrastLimits = new double[]{ displaysettings.min, displaysettings.max };
			//System.out.println( imageName + ": contrast limits = " + Arrays.toString( contrastLimits ) );
		}

		final ImageDisplay< ? > imageDisplay = new ImageDisplay<>( imageName, Arrays.asList( imageName ), color, contrastLimits );
		final View view = new View( imageName, "images", Arrays.asList( imageDisplay ), null, false );
		dataset.views().put( view.getName(), view );
	}

	private void addSegmentationView( AbstractSpimData< ? > spimData, int setupId, String name  )
	{
		final SegmentationDisplay< ? > display = new SegmentationDisplay<>( name, Arrays.asList( name ) );

		final BasicViewSetup viewSetup = spimData.getSequenceDescription().getViewSetupsOrdered().get( setupId );
		final double pixelWidth = viewSetup.getVoxelSize().dimension( 0 );
		display.setResolution3dView( new Double[]{ pixelWidth, pixelWidth, pixelWidth } );

		final View view = new View( name, "segmentations", Arrays.asList( display ), null, false );
		dataset.views().put( view.getName(), view );
	}

}
