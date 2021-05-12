package de.embl.cba.mobie2.transform;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.mobie2.MoBIE2;
import net.imglib2.FinalRealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import org.apache.commons.lang.ArrayUtils;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GridSourceTransformer extends AbstractSourceTransformer
{
	// Serialization
	private List< List< String > > sources;
	private String tableDataLocation;

	// Runtime
	private List< int[] > positions;
	private transient List< FinalRealInterval > intervals;
	private transient List< SourceAndConverter< ? > > transformedSources;

	@Override
	public List< SourceAndConverter< ? > > transform( List< SourceAndConverter< ? > > sourceAndConverters )
	{
		// Make a copy because not all sources in the input list may be transformed
		transformedSources = new CopyOnWriteArrayList<>( sourceAndConverters );
		intervals = new CopyOnWriteArrayList<>();

		if ( positions == null )
		{
			autoSetPositions();
		}

		final SourceAndConverter< ? > reference = getReferenceSource( sourceAndConverters );

		// TODO: make this work on the union of the sources
		FinalRealInterval bounds = Utils.estimateBounds( reference.getSpimSource() );
		final double spacingFactor = 0.1;
		double spacingX = ( 1.0 + spacingFactor ) * ( bounds.realMax( 0 ) - bounds.realMin( 0 ) );
		double spacingY = ( 1.0 + spacingFactor ) * ( bounds.realMax( 1 ) - bounds.realMin( 1 ) );

		final long start = System.currentTimeMillis();

		final int nThreads = MoBIE2.N_THREADS;
		final ExecutorService executorService = Executors.newFixedThreadPool( nThreads );
		for ( int gridIndex = 0; gridIndex < positions.size(); gridIndex++ )
		{
			final int gridPosition = gridIndex;

			executorService.execute( () -> {
				transformSourcesAtGridPosition( sourceAndConverters, transformedSources, spacingX, spacingY, gridPosition );
			} );
		}
		executorService.shutdown();
		try {
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
		}

		System.out.println( "Transformed " + sourceAndConverters.size() + " image source(s) in " + (System.currentTimeMillis() - start) + " ms, using " + nThreads + " thread(s)." );

		return transformedSources;
	}

	private void transformSourcesAtGridPosition( List< SourceAndConverter< ? > > sourceAndConverters, List< SourceAndConverter< ? > > transformedSources, double spacingX, double spacingY, int i )
	{
		final List< String > sources = this.sources.get( i );

		for ( String sourceName : sources )
		{
			final AffineTransform3D transform3D = new AffineTransform3D();
			transform3D.translate( spacingX * positions.get( i )[ 0 ], spacingY * positions.get( i )[ 1 ], 0 );

			final SourceAndConverter< ? > sourceAndConverter = Utils.getSource( sourceAndConverters, sourceName );

			if ( sourceAndConverter == null )
			{
				// This is OK, because the field `List< List< String > > sources`
				// can contain more sources than the ones that should be
				// transformed with `transform( List< SourceAndConverter< ? > > sourceAndConverters )`
				// Examples are multi-color images where there is a separate imageDisplay
				// for each color.
				continue;
			}

			final SourceAndConverter< ? > transformedSource = new SourceAffineTransformer( sourceAndConverter, transform3D ).getSourceOut();

			// Replace the original source by the transformed one
			transformedSources.remove( sourceAndConverter );
			transformedSources.add( transformedSource );

			sourceNameToTransform.put( sourceName, transform3D );
			intervals.add( Utils.estimateBounds( transformedSource.getSpimSource() ) );
		}
	}

	private SourceAndConverter< ? > getReferenceSource( List< SourceAndConverter< ? > > sourceAndConverters )
	{
		final List< String > sourcesAtFirstGridPosition = sources.get( 0 );

		for ( String name : sourcesAtFirstGridPosition )
		{
			final SourceAndConverter< ? > source = Utils.getSource( sourceAndConverters, name );
			if ( source != null )
			{
				return source;
			}
		}

		throw new UnsupportedOperationException( "None of the sources specified at the first grid position could be found at the list of the sources that are to be transformed. Names of sources at first grid position: " + ArrayUtils.toString( sourcesAtFirstGridPosition ) );
	}

	private void autoSetPositions()
	{
		final int numPositions = sources.size();
		final int numX = ( int ) Math.ceil( Math.sqrt( numPositions ) );
		positions = new ArrayList<>();
		int xIndex = 0;
		int yIndex = 0;
		for ( int i = 0; i < numPositions; i++ )
		{
			if ( xIndex == numX )
			{
				xIndex = 0;
				yIndex++;
			}
			positions.add( new int[]{ xIndex, yIndex }  );
			xIndex++;
		}
	}

	public String getTableDataLocation()
	{
		return tableDataLocation;
	}

	public List< FinalRealInterval > getIntervals()
	{
		return Collections.unmodifiableList( intervals );
	}
}
