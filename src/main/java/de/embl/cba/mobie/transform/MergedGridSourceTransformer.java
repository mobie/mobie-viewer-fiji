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
	public List< SourceAndConverter< T > > transform( List< SourceAndConverter< T > > sourceAndConverters )
	{
		final List< Source< T > > gridSources = new ArrayList<>();
		for ( String source : sources )
		{
			final SourceAndConverter< T > sourceAndConverter = Utils.getSourceAndConverter( sourceAndConverters, source );
			if ( sourceAndConverter != null )
				gridSources.add( sourceAndConverter.getSpimSource() );
		}

		if ( gridSources.size() == 0 )
		{
			// the transformer has nothing to transform
			return sourceAndConverters;
		}

		if ( positions == null )
			positions = createPositions( sources.size() );

		final MergedGridSource< T > mergedGridSource = new MergedGridSource<>( gridSources, positions, mergedGridSourceName );

		final VolatileSource< T, V > volatileMergedGridSource = new VolatileSource<>( mergedGridSource, MoBIE.sharedQueue );

		final SourceAndConverter< V > vsac = new SourceAndConverter<>( volatileMergedGridSource, ( Converter< V, ARGBType > ) Utils.getSourceAndConverter( sourceAndConverters, sources.get( 0 ) ).asVolatile().getConverter() );

		final SourceAndConverter< T > mergedSourceAndConverter = new SourceAndConverter( mergedGridSource, Utils.getSourceAndConverter( sourceAndConverters, sources.get( 0 ) ).getConverter(), vsac );

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
