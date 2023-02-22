package org.embl.mobie.lib.hcs;

import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.lib.annotation.AnnotatedRegion;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.serialize.ImageDataSource;
import org.embl.mobie.lib.serialize.RegionDataSource;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.lib.serialize.display.ImageDisplay;
import org.embl.mobie.lib.serialize.display.RegionDisplay;
import org.embl.mobie.lib.serialize.transformation.MergedGridTransformation;
import org.embl.mobie.lib.serialize.transformation.Transformation;
import org.embl.mobie.lib.table.ColumnNames;
import org.embl.mobie.lib.table.TableDataFormat;
import tech.tablesaw.api.IntColumn;
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
		final StorageLocation tableStorageLocation = new StorageLocation();
		final StringColumn wellColumn = StringColumn.create( ColumnNames.REGION_ID );
		final Table table = Table.create( hcsPlate.getName() );
		table.addColumns( wellColumn );
		tableStorageLocation.data = table;

		final RegionDataSource wellRegionDataSource = new RegionDataSource( hcsPlate.getName() );
		wellRegionDataSource.tableData = new HashMap<>();
		wellRegionDataSource.tableData.put( TableDataFormat.Table, tableStorageLocation );
		dataset.addDataSource( wellRegionDataSource );

		final RegionDisplay< AnnotatedRegion > wellRegionDisplay = new RegionDisplay<>( hcsPlate.getName() );
		wellRegionDisplay.tableSource = wellRegionDataSource.getName();
		wellRegionDisplay.sources = new HashMap<>();
		wellRegionDisplay.setBoundaryThickness( 10 ); // TODO: determine from well size
		final Set< String > channels = hcsPlate.getChannels();
		final String firstChannel = channels.iterator().next();

		for ( String channel : channels )
		{
			String metadataSiteSource = null;

			final ArrayList< Transformation > transformations = new ArrayList<>();

			final Set< String > wells = hcsPlate.getWells( channel );

			final MergedGridTransformation wellGrid = new MergedGridTransformation();
			wellGrid.sources = new ArrayList<>();
			wellGrid.positions = new ArrayList<>();
			wellGrid.setName( getChannelName( channel ) );

			for ( String well : wells )
			{
				final String wellName = getWellName( channel, well );

				// merge the sites within the well
				final MergedGridTransformation siteGrid = new MergedGridTransformation();
				siteGrid.sources = new ArrayList<>();
				siteGrid.positions = new ArrayList<>();
				siteGrid.setName( wellName );
				transformations.add( siteGrid );

				if( channel.equals( firstChannel ) )
				{
					// all channels should have the same wells,
					// thus we just use the first channel for the
					// well region display
					// (we could put all channels here, if needed)
					wellRegionDisplay.sources.put( well, Arrays.asList( wellName ) );
					wellColumn.append( well );
				}

				final Set< String > sites = hcsPlate.getSites( channel, well );
				for ( String site : sites )
				{
					// create site image source
					final StorageLocation storageLocation = new StorageLocation();
					storageLocation.absolutePath = hcsPlate.getPath( channel, well, site );
					storageLocation.channel = 0;
					// System.out.println( site + ":" + storageLocation.absolutePath );
					final ImageDataSource imageDataSource = new ImageDataSource( hcsPlate.getSiteKey( channel, well, site ), ImageDataFormat.ImageJ, storageLocation );
					dataset.addDataSource( imageDataSource );

					// add site image source to site grid
					siteGrid.sources.add( imageDataSource.getName() );
					siteGrid.positions.add( hcsPlate.getSiteGridPosition( channel, well, site ) );
					if ( metadataSiteSource == null )
					{
						// all sites should be identical, thus
						// we simply use the first one can be used for metadata
						metadataSiteSource = imageDataSource.getName();
					}
					siteGrid.metadataSource = metadataSiteSource;
				}

				// add the merged sites (= well) to the wells
				wellGrid.sources.add( wellName );
				wellGrid.positions.add( hcsPlate.getWellGridPosition( well ) );

				// add well view for testing
				//addWellView( hcsPlate, dataset, channel, wellName, siteGrid );
			}

			// create plate view for this channel
			// TODO: show all channels, because otherwise the region annotation does not work
			// TODO: add a viewer transformation to zoom to the first well
			transformations.add( wellGrid );
			String color = hcsPlate.getColor( channel );
			double[] contrastLimits = hcsPlate.getContrastLimits( channel );
			final ImageDisplay< ? > imageDisplay = new ImageDisplay<>( wellGrid.getName(), Arrays.asList( wellGrid.getName() ), color, contrastLimits );
			final View view = new View( wellGrid.getName(), "plate", Arrays.asList( imageDisplay, wellRegionDisplay ), transformations, true );
			dataset.views.put( view.getName(), view );
		}
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
