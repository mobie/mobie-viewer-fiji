package de.embl.cba.mobie.transform;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.Utils;
import net.imglib2.Volatile;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class MergedGridSourceTransformer< T extends NativeType< T > & NumericType< T > > extends AbstractSourceTransformer< T >
{
	// Serialization
	protected List< String > sources;
	protected String mergedGridSourceName;
	protected List< int[] > positions;
	protected boolean centerAtOrigin = false;

	@Override
	public List< SourceAndConverter< T > > transform( List< SourceAndConverter< T > > sourceAndConverters )
	{
		if ( positions == null )
			positions = createPositions( sources.size() );

		final List< Source< T > > gridSources = sources.stream().map( sourceName -> Utils.getSource( sourceAndConverters, sourceName ).getSpimSource() ).collect( Collectors.toList() );

		final List< Source< ? extends Volatile< T > > > volatileGridSources = sources.stream().map( sourceName -> Utils.getSource( sourceAndConverters, sourceName ).asVolatile().getSpimSource() ).collect( Collectors.toList() );

		final MergedGridSource< T > mergedGridSource = new MergedGridSource<>( gridSources, positions, mergedGridSourceName );

//		final MergedGridSource< ? extends Volatile< T > > volatileMergedGridSource = new MergedGridSource( volatileGridSources, positions, mergedGridSourceName );

		List< SourceAndConverter< T > > transformedSourceAndConverters = new CopyOnWriteArrayList<>( sourceAndConverters );



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
