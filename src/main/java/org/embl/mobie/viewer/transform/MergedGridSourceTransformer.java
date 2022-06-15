/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
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
package org.embl.mobie.viewer.transform;

import bdv.tools.transformation.TransformedSource;
import bdv.util.VolatileSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import net.imglib2.RealInterval;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MultiThreading;
import org.embl.mobie.viewer.source.FunctionGridSource;
import org.embl.mobie.viewer.source.MergedGridSource;
import org.embl.mobie.viewer.source.SourceHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class MergedGridSourceTransformer< T extends NumericType< T > >  extends AbstractSourceTransformer
{
	// Serialization
	protected List< String > sources;
	protected String mergedGridSourceName;
	protected List< int[] > positions;
	protected boolean centerAtOrigin = false; // TODO: should actually be true, but: https://github.com/mobie/mobie-viewer-fiji/issues/685#issuecomment-1108179599
	protected boolean encodeSource = false; // true for label images

	// Runtime
	private transient MergedGridSource< ? > mergedGridSource;
	private transient double[] translationRealOffset;
	private transient Set< SourceAndConverter > transformedSourceAndConverters;

	@Override
	public void transform( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter )
	{
		if ( centerAtOrigin == true )
		{
			// https://github.com/mobie/mobie-viewer-fiji/issues/685
			IJ.log( "[WARNING]: centerAtOrigin = true, is currently not properly working for the merged grid; will thus try with centerAtOrigin = false. See here: https://github.com/mobie/mobie-viewer-fiji/issues/685" );
			centerAtOrigin = false;
		}

		final long startTime = System.currentTimeMillis();

		final List< SourceAndConverter< ? > > gridSources = getGridSources( sourceNameToSourceAndConverter );

		if ( positions == null )
			positions = createPositions( gridSources.size() );

		SourceAndConverter< ? > mergedSourceAndConverter = createMergedSourceAndConverter( gridSources, gridSources.get( 0 ).asVolatile().getConverter(), gridSources.get( 0 ).getConverter() );

		SourceAndConverter< ? > functionSourceAndConverter = createFunctionGridSourceAndConverter( (List) gridSources, (Converter) gridSources.get( 0 ).asVolatile().getConverter(), (Converter) gridSources.get( 0 ).getConverter() );

		sourceNameToSourceAndConverter.put( functionSourceAndConverter.getSpimSource().getName(), functionSourceAndConverter );

		// Transform (i.e. adapt the positions) all contained sources,
		// because several parts of the code refer to them and their
		// positions.
		transformedSourceAndConverters = ConcurrentHashMap.newKeySet();
		transformContainedSources( sourceNameToSourceAndConverter, gridSources );
		mergedGridSource.setContainedSourceAndConverters( transformedSourceAndConverters );

		final long duration = System.currentTimeMillis() - startTime;
		if ( duration > MoBIE.minLogTimeMillis )
			IJ.log("Merged " + sources.size() + " sources into " + mergedGridSourceName + " in " + duration + " ms (centerAtOrigin="+centerAtOrigin+").");
	}

	private void transformContainedSources( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter, List< SourceAndConverter< ? > > gridSources )
	{
		final ArrayList< SourceAndConverter< ? > > referenceSources = new ArrayList<>();
		referenceSources.add( gridSources.get( 0 ) );

		final double[] gridCellRealDimensions = mergedGridSource.getCellRealDimensions();

		// account for grid margin
		translationRealOffset = computeTranslationOffset( gridSources, gridCellRealDimensions );

		final int numSources = gridSources.size();
		final ArrayList< Future< ? > > futures = MultiThreading.getFutures();
		for ( int positionIndex = 0; positionIndex < numSources; positionIndex++ )
		{
			final int finalPositionIndex = positionIndex;

			final ArrayList< String > sourceNamesAtGridPosition = getSourcesAtGridPosition( gridSources, finalPositionIndex );

			futures.add( MultiThreading.executorService.submit( () -> {
				recursivelyTransformSources( sourceNameToSourceAndConverter, gridCellRealDimensions, finalPositionIndex, sourceNamesAtGridPosition );
			} ) );
		}
		MultiThreading.waitUntilFinished( futures );
	}

	private double[] computeTranslationOffset( List< SourceAndConverter< ? > > gridSources, double[] gridCellRealDimensions )
	{
		final RealInterval dataRealBounds = SourceHelper.getMask( gridSources.get( 0 ).getSpimSource(), 0 );

		final double[] dataRealDimensions = new double[ 3 ];
		for ( int d = 0; d < 3; d++ )
			dataRealDimensions[ d ] = ( dataRealBounds.realMax( d ) - dataRealBounds.realMin( d ) );

		final double[] translationOffset = new double[ 2 ];
		for ( int d = 0; d < 2; d++ )
			translationOffset[ d ] = 0.5 * ( gridCellRealDimensions[ d ] - dataRealDimensions[ d ] );

		return translationOffset;
	}

	private void recursivelyTransformSources( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter, double[] gridCellRealDimensions, int finalPositionIndex, ArrayList< String > transformedSourceNames )
	{
		// transform the sources
		final double translationX = gridCellRealDimensions[ 0 ] * positions.get( finalPositionIndex )[ 0 ] + translationRealOffset[ 0 ];
		final double translationY = gridCellRealDimensions[ 1 ] * positions.get( finalPositionIndex )[ 1 ] + translationRealOffset[ 1 ];

		TransformedGridSourceTransformer.translate( sourceNameToSourceAndConverter, transformedSourceNames, null, centerAtOrigin, translationX, translationY );
		addTransformedSources( sourceNameToSourceAndConverter, transformedSourceNames );

		// if there are any, also transform contained sources
		final ArrayList< String > containedSourceNames = fetchContainedSourceNames( sourceNameToSourceAndConverter, transformedSourceNames );
		if ( containedSourceNames.size() > 0 )
		{
			recursivelyTransformSources( sourceNameToSourceAndConverter, gridCellRealDimensions, finalPositionIndex, containedSourceNames );
		}
	}

	private void addTransformedSources( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter, ArrayList< String > containedSourceNames )
	{
		final List< SourceAndConverter< ? > > sourceAndConverters = sourceNameToSourceAndConverter.values().stream().filter( sac -> containedSourceNames.contains( sac.getSpimSource().getName() ) ).collect( Collectors.toList() );
		transformedSourceAndConverters.addAll( sourceAndConverters );
	}

	private ArrayList< String > fetchContainedSourceNames( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter, ArrayList< String > sourceNames )
	{
		final ArrayList< String > containedSourceNames = new ArrayList<>();

		for ( String sourceName : sourceNames )
		{
			Source< ? > source = sourceNameToSourceAndConverter.get( sourceName ).getSpimSource();

			if ( source instanceof TransformedSource )
			{
				source = ( ( TransformedSource< ? > ) source ).getWrappedSource();
			}

			if ( source instanceof MergedGridSource )
			{
				containedSourceNames.addAll( ( ( MergedGridSource< ? > ) source ).getGridSources().stream().map( sac -> sac.getSpimSource().getName() ).collect( Collectors.toList() ) ) ;
			}
		}

		return containedSourceNames;
	}

	private ArrayList< String > getSourcesAtGridPosition( List< SourceAndConverter< ? > > gridSources, int finalPositionIndex )
	{
		final ArrayList< String > sourcesAtGridPosition = new ArrayList<>();
		sourcesAtGridPosition.add( gridSources.get( finalPositionIndex ).getSpimSource().getName() );
		return sourcesAtGridPosition;
	}

	@Override
	public List< String > getSources()
	{
		return sources;
	}

	private SourceAndConverter< ? > createMergedSourceAndConverter( List< SourceAndConverter< ? > > gridSources, Converter< ?, ARGBType > volatileConverter, Converter< ?, ARGBType > converter )
	{
		mergedGridSource = new MergedGridSource( gridSources, positions, mergedGridSourceName, TransformedGridSourceTransformer.RELATIVE_CELL_MARGIN, encodeSource );

		final VolatileSource< ?, ? > volatileMergedGridSource = new VolatileSource<>( mergedGridSource, MultiThreading.sharedQueue );

		final SourceAndConverter< ? > volatileSourceAndConverter = new SourceAndConverter( volatileMergedGridSource, volatileConverter );

		final SourceAndConverter< ? > mergedSourceAndConverter = new SourceAndConverter( mergedGridSource, converter, volatileSourceAndConverter );

		return mergedSourceAndConverter;
	}

	private SourceAndConverter< T > createFunctionGridSourceAndConverter( List< SourceAndConverter< T > > gridSources, Converter< T, ARGBType > volatileConverter, Converter< T, ARGBType > converter )
	{
		final List< Source< T > > source = gridSources.stream().map( sac -> sac.getSpimSource() ).collect( Collectors.toList() );

		final FunctionGridSource gridSource = new FunctionGridSource( source, positions, mergedGridSourceName, TransformedGridSourceTransformer.RELATIVE_CELL_MARGIN, encodeSource );

		final List< ? extends Source< ? extends Volatile< T > > > volatileSources = gridSources.stream().map( sac -> sac.asVolatile().getSpimSource() ).collect( Collectors.toList() );

		final FunctionGridSource volatileGridSource = new FunctionGridSource( volatileSources, positions, mergedGridSourceName, TransformedGridSourceTransformer.RELATIVE_CELL_MARGIN, encodeSource );

		final SourceAndConverter volatileGridSac = new SourceAndConverter( volatileGridSource, volatileConverter );

		final SourceAndConverter< T > gridSac = new SourceAndConverter( gridSource, converter, volatileGridSac );

		return gridSac;
	}


	private List< SourceAndConverter< ? > > getGridSources( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter )
	{
		final List< SourceAndConverter< ? > > gridSources = new ArrayList<>();
		for ( String sourceName : sources )
		{
			gridSources.add( sourceNameToSourceAndConverter.get( sourceName ) );
		}
		return gridSources;
	}

	private static List< int[] > createPositions( int size )
	{
		final int numPositions = size;
		final int numX = ( int ) Math.ceil( Math.sqrt( numPositions ) );
		List< int[] > positions = new ArrayList<>();
		int xPositionIndex = 0;
		int yPositionIndex = 0;
		for ( int gridIndex = 0; gridIndex < numPositions; gridIndex++ )
		{
			if ( xPositionIndex == numX )
			{
				xPositionIndex = 0;
				yPositionIndex++;
			}
			positions.add( new int[]{ xPositionIndex, yPositionIndex }  );
			xPositionIndex++;
		}

		return positions;
	}
}
