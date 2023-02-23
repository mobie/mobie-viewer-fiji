package org.embl.mobie.lib.hcs;

import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.lib.annotation.AnnotatedRegion;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.serialize.ImageDataSource;
import org.embl.mobie.lib.serialize.RegionDataSource;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.lib.serialize.display.Display;
import org.embl.mobie.lib.serialize.display.ImageDisplay;
import org.embl.mobie.lib.serialize.display.RegionDisplay;
import org.embl.mobie.lib.serialize.transformation.MergedGridTransformation;
import org.embl.mobie.lib.serialize.transformation.Transformation;
import org.embl.mobie.lib.table.ColumnNames;
import org.embl.mobie.lib.table.TableDataFormat;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

public class HCSDataSetter
{
	/**
	 * Adds the content of the {@code hcsPlate} to the {@code dataset}.
	 *
	 * @param hcsPlate
	 * 					a HCSPlate
	 * @param dataset
	 * 					the current MoBIE dataset
	 */
	public void addPlateToDataset( HCSPlate hcsPlate, Dataset dataset )
	{
		final Set< String > channels = hcsPlate.getChannels();
		final String firstChannel = channels.iterator().next();

		// init a RegionDisplay for navigating the wells
		final RegionDisplay< AnnotatedRegion > wellRegionDisplay = new RegionDisplay<>( "wells" );
		wellRegionDisplay.sources = new HashMap<>();
		wellRegionDisplay.showAsBoundaries( true );
		wellRegionDisplay.setBoundaryThickness( ( float ) (0.1 * hcsPlate.getSiteRealDimensions( firstChannel )[ 0 ]) );

		final ArrayList< Transformation > transformations = new ArrayList<>();
		final ArrayList< Display< ? > > displays = new ArrayList<>();

		for ( String channel : channels )
		{
			String metadataSiteSource = null;

			final Set< String > wells = hcsPlate.getWells( channel );

			final MergedGridTransformation wellGrid = new MergedGridTransformation();
			wellGrid.sources = new ArrayList<>();
			wellGrid.positions = new ArrayList<>();
			wellGrid.setName( getChannelName( channel ) );

			for ( String well : wells )
			{
				final String wellName = getWellName( channel, well );

				// init grid for merging sites within the well
				final MergedGridTransformation siteGrid = new MergedGridTransformation();
				siteGrid.sources = new ArrayList<>();
				siteGrid.positions = new ArrayList<>();
				siteGrid.setName( wellName );

				if( channel.equals( firstChannel ) )
				{
					// all channels should have the same wells,
					// thus we simply use the first channel for the
					// well region display
					wellRegionDisplay.sources.put( well, Arrays.asList( wellName ) );
				}

				// for each site, create an image source and add it to the site grid
				final Set< String > sites = hcsPlate.getSites( channel, well );
				for ( String site : sites )
				{
					// create site image source
					final StorageLocation storageLocation = new StorageLocation();
					storageLocation.absolutePath = hcsPlate.getPath( channel, well, site );
					storageLocation.channel = 0;
					final ImageDataSource imageDataSource = new ImageDataSource( hcsPlate.getSiteKey( channel, well, site ), ImageDataFormat.ImageJ, storageLocation );
					dataset.addDataSource( imageDataSource );

					// add site image source to site grid
					siteGrid.sources.add( imageDataSource.getName() );
					siteGrid.positions.add( hcsPlate.getSiteGridPosition( channel, well, site ) );
					if ( metadataSiteSource == null )
					{
						// all sites should be identical, thus
						// we simply use the first site of
						// this channel for metadata
						metadataSiteSource = imageDataSource.getName();
					}

					siteGrid.metadataSource = metadataSiteSource;
				}

				transformations.add( siteGrid );

				// add the merged sites to the well grid
				wellGrid.sources.add( wellName );
				wellGrid.positions.add( hcsPlate.getWellGridPosition( well ) );

				// add well view for testing
				//addWellView( hcsPlate, dataset, channel, wellName, siteGrid );
			}

			transformations.add( wellGrid );

			String color = hcsPlate.getColor( channel );
			double[] contrastLimits = hcsPlate.getContrastLimits( channel );
			final ImageDisplay< ? > imageDisplay = new ImageDisplay<>( wellGrid.getName(), Arrays.asList( wellGrid.getName() ), color, contrastLimits );
			displays.add( imageDisplay );
		}

		displays.add( wellRegionDisplay );

		// create plate view
		final View view = new View( hcsPlate.getName(), "plate", displays, transformations, true );
		dataset.views.put( view.getName(), view );
	}

	// method currently only used for testing, could be removed at some point
	private void addWellView( HCSPlate hcsPlate, Dataset dataset, String channel, String wellName, MergedGridTransformation siteGrid )
	{
		String color = hcsPlate.getColor( channel );
		double[] contrastLimits = hcsPlate.getContrastLimits( channel );
		final ImageDisplay< ? > imageDisplay = new ImageDisplay<>( wellName, Arrays.asList( wellName ), color, contrastLimits );
		final View view = new View( wellName, "well", Arrays.asList( imageDisplay ), Arrays.asList( siteGrid ), true );
		dataset.views.put( view.getName(), view );
	}

	private String getWellName( String channel, String well )
	{
		return getChannelName( channel ) + "--w_" + well;
	}

	private String getChannelName( String channel )
	{
		return "c_" + channel;
	}
}
