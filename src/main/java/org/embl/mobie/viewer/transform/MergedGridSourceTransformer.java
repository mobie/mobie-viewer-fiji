package org.embl.mobie.viewer.transform;

import bdv.tools.transformation.TransformedSource;
import bdv.util.VolatileSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.Logger;
import org.embl.mobie.viewer.MoBIEHelper;
import net.imglib2.FinalRealInterval;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import org.embl.mobie.viewer.ThreadUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class MergedGridSourceTransformer extends AbstractSourceTransformer
{
	// Serialization
	protected List< String > sources;
	protected String mergedGridSourceName;
	protected List< int[] > positions;
	protected boolean centerAtOrigin = false;
	protected boolean encodeSource = false;

	// Runtime
	private transient MergedGridSource< ? > mergedGridSource;
	private transient double[] translationRealOffset;
	private transient Set< SourceAndConverter > transformedSourceAndConverters;

	@Override
	public void transform( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter )
	{
		Logger.info("Merging " + sources.size() + " sources into " + mergedGridSourceName );

		final List< SourceAndConverter< ? > > gridSources = getGridSources( sourceNameToSourceAndConverter );

		if ( positions == null )
			positions = createPositions( gridSources.size() );

		SourceAndConverter< ? > mergedSourceAndConverter = createMergedSourceAndConverter( gridSources.stream().map( sac -> sac.getSpimSource() ).collect( Collectors.toList() ), gridSources.get( 0 ).asVolatile().getConverter(), gridSources.get( 0 ).getConverter() );

		sourceNameToSourceAndConverter.put( mergedSourceAndConverter.getSpimSource().getName(), mergedSourceAndConverter );

		// Transform (i.e. adapt the positions) all contained sources,
		// because several parts of the code refer to them and their
		// positions.
		transformedSourceAndConverters = ConcurrentHashMap.newKeySet();
		transformContainedSources( sourceNameToSourceAndConverter, gridSources );
		mergedGridSource.setContainedSourceAndConverters( transformedSourceAndConverters );
	}

	private void transformContainedSources( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter, List< SourceAndConverter< ? > > gridSources )
	{
		final ArrayList< SourceAndConverter< ? > > referenceSources = new ArrayList<>();
		referenceSources.add( gridSources.get( 0 ) );

		final double[] gridCellRealMax = mergedGridSource.getCellRealMax();

		// account for grid margin
		translationRealOffset = computeTranslationOffset( gridSources, gridCellRealMax );

		final int numSources = gridSources.size();
		final ArrayList< Future< ? > > futures = ThreadUtils.getFutures();
		for ( int positionIndex = 0; positionIndex < numSources; positionIndex++ )
		{
			final int finalPositionIndex = positionIndex;

			final ArrayList< String > sourceNamesAtGridPosition = getSourcesAtGridPosition( gridSources, finalPositionIndex );

			futures.add( ThreadUtils.executorService.submit( () -> {
				recursivelyTransformSources( sourceNameToSourceAndConverter, gridCellRealMax, finalPositionIndex, sourceNamesAtGridPosition );
			} ) );
		}
		ThreadUtils.waitUntilFinished( futures );
	}

	private double[] computeTranslationOffset( List< SourceAndConverter< ? > > gridSources, double[] gridCellRealMax )
	{
		final FinalRealInterval dataRealBounds = MoBIEHelper.estimateBounds( gridSources.get( 0 ).getSpimSource(), 0 );

		final double[] dataRealDimensions = new double[ 3 ];
		for ( int d = 0; d < 3; d++ )
			dataRealDimensions[ d ] = ( dataRealBounds.realMax( d ) - dataRealBounds.realMin( d ) );

		final double[] translationOffset = new double[ 2 ];
		for ( int d = 0; d < 2; d++ )
			translationOffset[ d ] = 0.5 * ( gridCellRealMax[ d ] - dataRealDimensions[ d ] );

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
				containedSourceNames.addAll( ( ( MergedGridSource< ? > ) source ).getGridSources().stream().map( s -> s.getName() ).collect( Collectors.toList() ) ) ;
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

	private SourceAndConverter< ? > createMergedSourceAndConverter( List< Source< ? > > gridSources, Converter< ?, ARGBType > volatileConverter, Converter< ?, ARGBType > converter )
	{
		mergedGridSource = new MergedGridSource( gridSources, positions, mergedGridSourceName, TransformedGridSourceTransformer.RELATIVE_CELL_MARGIN, encodeSource );

		final VolatileSource< ?, ? > volatileMergedGridSource = new VolatileSource<>( mergedGridSource, ThreadUtils.sharedQueue );

		final SourceAndConverter< ? > volatileSourceAndConverter = new SourceAndConverter( volatileMergedGridSource, volatileConverter );

		final SourceAndConverter< ? > mergedSourceAndConverter = new SourceAndConverter( mergedGridSource, converter, volatileSourceAndConverter );

		return mergedSourceAndConverter;
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
