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
package org.embl.mobie.lib.hcs;

import ij.IJ;
import net.thisptr.jackson.jq.internal.misc.Strings;
import org.embl.mobie.lib.annotation.AnnotatedRegion;
import org.embl.mobie.lib.annotation.AnnotatedSegment;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.serialize.ImageDataSource;
import org.embl.mobie.lib.serialize.SegmentationDataSource;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.lib.serialize.display.Display;
import org.embl.mobie.lib.serialize.display.ImageDisplay;
import org.embl.mobie.lib.serialize.display.RegionDisplay;
import org.embl.mobie.lib.serialize.display.SegmentationDisplay;
import org.embl.mobie.lib.serialize.transformation.MergedGridTransformation;
import org.embl.mobie.lib.serialize.transformation.Transformation;
import org.embl.mobie.lib.transform.viewer.ImageZoomViewerTransform;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

public class HCSDataAdder
{
	private final Plate plate;
	private final double wellMargin;
	private final double siteMargin;

	public HCSDataAdder( Plate plate, double wellMargin, double siteMargin  )
	{
		this.plate = plate;
		this.wellMargin = wellMargin;
		this.siteMargin = siteMargin;
	}

	public void addData( Dataset dataset )
	{
		boolean is2D = plate.is2D();
		dataset.is2D( is2D );

		final Set< Channel > channels = plate.getChannels();
		final Channel firstChannel = channels.iterator().next();

		// build a RegionDisplay for outlining
		// and navigating the wells
		//
		final RegionDisplay< AnnotatedRegion > wellDisplay = new RegionDisplay<>( "wells" );
		wellDisplay.sources = new LinkedHashMap<>();
		wellDisplay.showAsBoundaries( true );
		wellDisplay.setBoundaryThickness( 0.1 );
		wellDisplay.boundaryThicknessIsRelative( true );
		wellDisplay.setRelativeDilation( 0.1 );

		// wells should be displayed for all time-points.
		// currently, the below code assumes that the time-points
		// are sequential, starting at 0; if needed one could be
		// more sophisticated here, because the Source
		// data model of BDV allows for missing time-points and for
		// time-sequences that do not (all) need to start at 0
		final int numTimepoints = plate.getTPositions().size();
		for ( int t = 0; t < numTimepoints; t++ )
		{
			wellDisplay.timepoints().add( t );
		}


		// build nested grid views of the sites and wells for all channels
		//
		final ArrayList< Transformation > imageTransforms = new ArrayList<>();
		final ArrayList< Display< ? > > displays = new ArrayList<>();

		for ( Channel channel : channels )
		{
			String metadataSiteSource = null;

			final Set< Well > wells = plate.getWells( channel );

			final MergedGridTransformation wellGrid = new MergedGridTransformation( "ch_" + channel.getName() );
			wellGrid.sources = new ArrayList<>();
			wellGrid.positions = new ArrayList<>();
			wellGrid.margin = wellMargin;

			for ( Well well : wells )
			{
				String wellID = getWellID( plate, channel, well );

				MergedGridTransformation siteGrid = null;
				if ( plate.getSitesPerWell() > 1 )
				{
					// init grid for merging sites within the well
					siteGrid = new MergedGridTransformation( wellID );
					siteGrid.sources = new ArrayList<>();
					siteGrid.positions = new ArrayList<>();
					siteGrid.margin = siteMargin;
				}

				if( channel.equals( firstChannel ) )
				{
					// all channels should have the same wells,
					// thus we simply and only use
					// the first channel for the
					// well region display
					wellDisplay.sources.put( wellID, Arrays.asList( wellID ) );
				}

				// for each site, create an image source
				// and add it to the site grid
				//
				final Set< Site > sites = plate.getSites( channel, well );
				for ( Site site : sites )
				{
					if ( plate.getSitesPerWell() > 1 )
					{
						// create a site grid to form the well
						//
						String siteID = getSiteID( plate, channel, well, site );
						ImageDataSource imageDataSource = createImageDataSource( channel, site, siteID );
						dataset.addDataSource( imageDataSource );

						// add site image source to site grid
						siteGrid.sources.add( imageDataSource.getName() );
						siteGrid.positions.add( plate.getGridPosition( site ) );

						if ( metadataSiteSource == null )
						{
							// all sites should be identical, thus
							// we simply use the first site of
							// this channel for metadata
							metadataSiteSource = imageDataSource.getName();
						}

						siteGrid.metadataSource = metadataSiteSource;
					}
					else
					{
						// the one site is the well
						//
						ImageDataSource imageDataSource = createImageDataSource( channel, site, wellID );
						dataset.addDataSource( imageDataSource );
					}
				}

				if ( siteGrid != null )
					imageTransforms.add( siteGrid );

				// add the merged site grid,
				// of name wellID,
				// to the well grid
				wellGrid.sources.add( wellID );
				wellGrid.positions.add( plate.getWellGridPosition( well ) );
			}

			imageTransforms.add( wellGrid );

			if ( channel.getName().equals( "labels" ) )
			{
				SegmentationDisplay< AnnotatedSegment > segmentationDisplay = new SegmentationDisplay<>( wellGrid.getName(), Arrays.asList( wellGrid.getName() ) );
				displays.add( segmentationDisplay );
			}
			else
			{
				ImageDisplay< ? > imageDisplay = new ImageDisplay<>( wellGrid.getName(), Arrays.asList( wellGrid.getName() ), channel.getColor(), channel.getContrastLimits() );
				displays.add( imageDisplay );
			}

		}

		displays.add( wellDisplay );

		// create plate view
		final ArrayList< String > wells = new ArrayList<>( wellDisplay.sources.keySet() );
		Collections.sort( wells );
		final ImageZoomViewerTransform viewerTransform = new ImageZoomViewerTransform( wells.get( 0 ), 0 );
		final View view = new View( plate.getName(), "plate", displays, imageTransforms, viewerTransform, true, null );
		dataset.views().put( view.getName(), view );
	}

	@NotNull
	private static ImageDataSource createImageDataSource( Channel channel, Site site, String siteID )
	{
		ImageDataSource imageDataSource;
		if ( channel.getName().equals( "labels" ) )
		{
			imageDataSource = new SegmentationDataSource( siteID, site.getImageDataFormat(), site );
		}
		else
		{
			imageDataSource = new ImageDataSource( siteID, site.getImageDataFormat(), site );
		}
		return imageDataSource;
	}

	private String getSiteID( Plate plate, Channel channel, Well well, Site site )
	{
		return getWellID( plate, channel, well ) + "-s" + site.getId();
	}

	private String getWellID( Plate plate, Channel channel, Well well )
	{
		return Strings.join( "-", Arrays.asList( plate.getName(), "ch" + channel.getName(), "w" + well.getName() ) );
	}
}
