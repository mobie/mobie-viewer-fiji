package de.embl.cba.mobie.transform;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.Utils;
import net.imglib2.RealInterval;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MergedGridSourceTransformer< T extends NumericType< T > > extends AbstractSourceTransformer< T >
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
		{
			// TODO: Put this into AbstractSourceTransformer?!
			autoSetPositions();
		}

		final List< Source< T > > gridSources = sources.stream().map( sourceName -> Utils.getSource( sourceAndConverters, sourceName ).getSpimSource() ).collect( Collectors.toList() );

		final List< Source< ? extends Volatile< T > > > volatileGridSources = sources.stream().map( sourceName -> Utils.getSource( sourceAndConverters, sourceName ).asVolatile().getSpimSource() ).collect( Collectors.toList() );

		final MergedGridSource< T > mergedGridSource = new MergedGridSource<>( gridSources, positions, mergedGridSourceName );

		final MergedGridSource< ? extends Volatile< T > > volatileMergedGridSource = new MergedGridSource( volatileGridSources, positions, mergedGridSourceName );

		List< SourceAndConverter< T > > transformedSourceAndConverters = new CopyOnWriteArrayList<>( sourceAndConverters );

		return transformedSourceAndConverters;
	}

	private void autoSetPositions()
	{
		final int numPositions = sources.size();
		final int numX = ( int ) Math.ceil( Math.sqrt( numPositions ) );
		positions = new LinkedHashMap<>();
		int xPositionIndex = 0;
		int yPositionIndex = 0;
		for ( int gridIndex = 0; gridIndex < numPositions; gridIndex++ )
		{
			if ( xPositionIndex == numX )
			{
				xPositionIndex = 0;
				yPositionIndex++;
			}
			positions.put( gridIds.get( gridIndex ), new int[]{ xPositionIndex, yPositionIndex }  );
			xPositionIndex++;
		}
	}
}
