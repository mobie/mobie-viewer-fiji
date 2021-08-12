package de.embl.cba.mobie.transform;

import bdv.util.VolatileSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.Utils;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class MergedGridSourceTransformer< T extends NativeType< T > & NumericType< T >, V extends Volatile< T > & NumericType< V > > extends AbstractSourceTransformer< T >
{
	// Serialization
	protected List< String > sources;
	protected String mergedGridSourceName;
	protected List< int[] > positions;
	protected boolean centerAtOrigin = false;

	@Override
	public void transform( Map< String, SourceAndConverter< T > > sourceNameToSourceAndConverter )
	{
		final List< SourceAndConverter< T > > gridSources = getGridSources( sourceNameToSourceAndConverter );

		if ( positions == null )
			positions = createPositions( gridSources.size() );

		final SourceAndConverter< T > mergedSourceAndConverter = createMergedSourceAndConverter( gridSources.stream().map( sac -> sac.getSpimSource() ).collect( Collectors.toList() ), ( Converter< V, ARGBType > ) gridSources.get( 0 ).asVolatile().getConverter(), gridSources.get( 0 ).getConverter() );

		// TODO: Maybe also transform the individual sources as in the GridSourceTransformer such that we know where they are?!

		sourceNameToSourceAndConverter.put( mergedSourceAndConverter.getSpimSource().getName(), mergedSourceAndConverter );
	}

	@Override
	public List< String > getSources()
	{
		return sources;
	}

	private SourceAndConverter< T > createMergedSourceAndConverter( List< Source< T > > gridSources, Converter< V, ARGBType > volatileConverter, Converter< T, ARGBType > converter )
	{
		final MergedGridSource< T > mergedGridSource = new MergedGridSource<>( gridSources, positions, mergedGridSourceName, 0.10 );

		final VolatileSource< T, V > volatileMergedGridSource = new VolatileSource<>( mergedGridSource, MoBIE.sharedQueue );

		final SourceAndConverter< V > volatileSourceAndConverter = new SourceAndConverter<>( volatileMergedGridSource, volatileConverter );

		final SourceAndConverter< T > mergedSourceAndConverter = new SourceAndConverter( mergedGridSource, converter, volatileSourceAndConverter );

		return mergedSourceAndConverter;
	}

	private List< SourceAndConverter< T > > getTransformedSourceAndConverters( List< SourceAndConverter< T > > sourceAndConverters, SourceAndConverter< T > mergedSourceAndConverter )
	{
		List< SourceAndConverter< T > > transformedSourceAndConverters = new CopyOnWriteArrayList<>( sourceAndConverters );
		transformedSourceAndConverters.add( mergedSourceAndConverter );
		// remove the merged sources
		for ( String source : sources )
		{
			final SourceAndConverter< T > sourceAndConverter = Utils.getSourceAndConverter( transformedSourceAndConverters, source );
			if ( sourceAndConverter != null )
				transformedSourceAndConverters.remove( sourceAndConverter );
		}
		return transformedSourceAndConverters;
	}

	private List< SourceAndConverter< T > > getGridSources( Map< String, SourceAndConverter< T > > sourceNameToSourceAndConverter )
	{
		final List< SourceAndConverter< T > > gridSources = new ArrayList<>();
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
