/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.lib;

import org.apache.commons.io.FilenameUtils;
import org.embl.mobie.MoBIESettings;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.imagedata.ImageData;
import org.embl.mobie.lib.color.ColorHelper;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.serialize.DataSource;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.serialize.ImageDataSource;
import org.embl.mobie.lib.serialize.SegmentationDataSource;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.lib.serialize.display.ImageDisplay;
import org.embl.mobie.lib.serialize.display.SegmentationDisplay;
import org.embl.mobie.lib.table.TableDataFormat;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalDatasetMetadata;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

public class ImageDataAdder
{
	private final ImageData< ? > image;
	private final ImageData< ? > labels;
	private final StorageLocation tableStorageLocation;
	private final TableDataFormat tableDataFormat;
	private Dataset dataset;
	private MoBIESettings settings;

	public ImageDataAdder( ImageData< ? > image, ImageData< ? > labels, StorageLocation tableStorageLocation, TableDataFormat tableDataFormat )
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

		addData( image, false );

		if ( labels != null )
			addData( labels, true );
	}

	private void addData( ImageData< ? > imageData, boolean isSegmentation )
	{
		final ImageDataFormat imageDataFormat = ImageDataFormat.ImageData;

		if ( tableDataFormat != null )
			settings.addTableDataFormat( tableDataFormat );

		// I need numSetups and setupNames
		final int numDatasets = imageData.getNumDatasets();

		for ( int datasetIndex = 0; datasetIndex < numDatasets; datasetIndex++ )
		{
			final StorageLocation storageLocation = new StorageLocation();
			storageLocation.data = imageData;
			storageLocation.setChannel( datasetIndex );
			final String datasetName = imageData.getSourcePair( datasetIndex ).getB().getName();
			String imageName = getImageName( datasetName, numDatasets );

			DataSource dataSource;
			if ( isSegmentation )
			{
				dataSource = new SegmentationDataSource( imageName, imageDataFormat, storageLocation, tableDataFormat, tableStorageLocation );
				addSegmentationView( imageData, datasetIndex, imageName );
			}
			else
			{
				dataSource = new ImageDataSource( imageName, imageDataFormat, storageLocation );
				addImageView( imageData, datasetIndex, imageName );
			}

			dataSource.preInit( true );
			dataset.putDataSource( dataSource );
			dataset.is2D( MoBIEHelper.is2D( imageData, datasetIndex ) );
		}
	}

	private String getImageName( String setupName, int numImages )
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

	private void addImageView( ImageData< ? > imageData, int datasetIndex, String imageName )
	{
		CanonicalDatasetMetadata metadata = imageData.getMetadata( datasetIndex );

		final ImageDisplay< ? > imageDisplay = new ImageDisplay<>(
				imageName,
                Collections.singletonList( imageName ),
				ColorHelper.getString( metadata.getColor() ),
				new double[]{ metadata.minIntensity(), metadata.minIntensity() } );

		final View view = new View(
				imageName,
				"images",
                Collections.singletonList( imageDisplay ),
				null,
				null,
				false,
				null );

		dataset.views().put( view.getName(), view );
	}

	private void addSegmentationView( ImageData< ? > imageData, int setupId, String name )
	{
		final SegmentationDisplay< ? > display = new SegmentationDisplay<>( name, Arrays.asList( name ) );
		final double pixelWidth = imageData.getSourcePair( setupId ).getB().getVoxelDimensions().dimension( 0 );
		display.setResolution3dView( new Double[]{ pixelWidth, pixelWidth, pixelWidth } );

		final View view = new View( name, "segmentations", Arrays.asList( display ), null, null, false, null );
		dataset.views().put( view.getName(), view );
	}

}
