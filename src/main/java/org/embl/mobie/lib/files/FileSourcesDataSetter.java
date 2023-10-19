/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
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
package org.embl.mobie.lib.files;

import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.lib.DataStore;
import org.embl.mobie.lib.annotation.AnnotatedRegion;
import org.embl.mobie.lib.annotation.AnnotatedSegment;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.serialize.ImageDataSource;
import org.embl.mobie.lib.serialize.RegionDataSource;
import org.embl.mobie.lib.serialize.SegmentationDataSource;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.lib.serialize.display.Display;
import org.embl.mobie.lib.serialize.display.ImageDisplay;
import org.embl.mobie.lib.serialize.display.RegionDisplay;
import org.embl.mobie.lib.serialize.display.SegmentationDisplay;
import org.embl.mobie.lib.serialize.transformation.MergedGridTransformation;
import org.embl.mobie.lib.source.Metadata;
import org.embl.mobie.lib.table.TableDataFormat;
import org.embl.mobie.lib.table.TableSource;
import org.embl.mobie.lib.transform.GridType;
import org.embl.mobie.lib.transform.viewer.ImageZoomViewerTransform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public class FileSourcesDataSetter
{
	private final List< ImageFileSources > images;
	private final List< LabelFileSources > labels;

	public FileSourcesDataSetter( List< ImageFileSources > images, List< LabelFileSources > labels )
	{
		this.images = images;
		this.labels = labels;
	}

	public void addData( Dataset dataset )
	{
		final ArrayList< ImageFileSources > allSources = new ArrayList<>();
		allSources.addAll( images );
		allSources.addAll( labels );

		// create and add data sources to the dataset
		for ( ImageFileSources sources : allSources )
		{
			for ( String name : sources.getSources() )
			{
				final String path = sources.getPath( name );
				ImageDataFormat imageDataFormat = ImageDataFormat.fromPath( path );

				final StorageLocation storageLocation = new StorageLocation();
				storageLocation.absolutePath = path;
				storageLocation.setChannel( sources.getChannelIndex() );
				if ( sources instanceof LabelFileSources )
				{
					final TableSource tableSource = ( ( LabelFileSources ) sources ).getLabelTable( name );
					SegmentationDataSource segmentationDataSource = SegmentationDataSource.create( name, imageDataFormat, storageLocation, tableSource );
					segmentationDataSource.preInit( false );
					dataset.addDataSource( segmentationDataSource );
				}
				else
				{
					final ImageDataSource imageDataSource = new ImageDataSource( name, imageDataFormat, storageLocation );
					imageDataSource.preInit( false );
					dataset.addDataSource( imageDataSource );
				}
			}
		}

		// TODO don't create a grid view if there is only one image and label mask
		final ImageFileSources firstImageFileSources = allSources.get( 0 );

		for ( ImageFileSources sources : allSources )
		{
			if ( sources.getGridType().equals( GridType.Stitched ) )
			{
				final ArrayList< Display< ? > > displays = new ArrayList<>();

				// TODO: probably we should not even create the region table
				//   for any source other than the first one while
				//   creating the ImageSources
				if ( sources.equals( firstImageFileSources ) )
				{
					// create a RegionDisplay

					// init table for the RegionDisplay
					final StorageLocation storageLocation = new StorageLocation();
					storageLocation.data = sources.getRegionTable();
					final RegionDataSource regionDataSource = new RegionDataSource( sources.getName() );
					regionDataSource.addTable( TableDataFormat.Table, storageLocation );
					DataStore.putRawData( regionDataSource );

					// init RegionDisplay
					final RegionDisplay< AnnotatedRegion > regionDisplay = new RegionDisplay<>( "image table" );
					regionDisplay.sources = new LinkedHashMap<>();
					regionDisplay.tableSource = regionDataSource.getName();
					regionDisplay.showAsBoundaries( true );
					regionDisplay.setBoundaryThickness( 0.015 );
					regionDisplay.boundaryThicknessIsRelative( true );
					regionDisplay.setRelativeDilation( 0.025 );
					regionDisplay.setOpacity( 1.0 );

					Integer numTimePoints = sources.getMetadata().numTimePoints;
					if ( numTimePoints == null )
						numTimePoints = 1000; // TODO
					for ( int t = 0; t < numTimePoints; t++ )
						regionDisplay.timepoints().add( t );

					final List< String > sourceNames = sources.getSources();
					final int numRegions = sourceNames.size();
					for ( int regionIndex = 0; regionIndex < numRegions; regionIndex++ )
						regionDisplay.sources.put( sourceNames.get( regionIndex ), Collections.singletonList( sourceNames.get( regionIndex ) ) );

					displays.add( regionDisplay );
				}

				// create grid transformation
				final MergedGridTransformation grid = new MergedGridTransformation( sources.getName() );
				grid.sources = sources.getSources();
				grid.metadataSource = sources.getMetadataSource();
				// TODO https://github.com/mobie/mobie-viewer-fiji/issues/1035
				grid.lazyLoadTables = false;

				if ( sources instanceof LabelFileSources )
				{
					// SegmentationDisplay
					final SegmentationDisplay< AnnotatedSegment > segmentationDisplay = new SegmentationDisplay<>( grid.getName(), Collections.singletonList( grid.getName() ) );
					final int numLabelTables = ( ( LabelFileSources ) sources ).getNumLabelTables();
					segmentationDisplay.setShowTable( numLabelTables > 0 );
					displays.add( segmentationDisplay );
				}
				else
				{
					// ImageDisplay
					final Metadata metadata = sources.getMetadata();
					displays.add( new ImageDisplay<>( grid.getName(), Collections.singletonList( grid.getName() ), metadata.color, metadata.contrastLimits ) );
				}

				// create grid view
				//
				final ImageZoomViewerTransform viewerTransform = new ImageZoomViewerTransform( grid.getSources().get( 0 ), 0 );
				final View gridView = new View( sources.getName(), "data", displays, Arrays.asList( grid ), viewerTransform, false );
				//gridView.overlayNames( true ); // Timepoint bug:
				dataset.views().put( gridView.getName(), gridView );
			}
			else
			{
				throw new UnsupportedOperationException( "Grid type not yet supported.");
			}
		}
	}
}
