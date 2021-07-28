package de.embl.cba.mobie.transform;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.Utils;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GridSourceTransformer< T extends NumericType< T > > extends AbstractSourceTransformer< T >
{
	// Serialization
	protected LinkedHashMap< String, List< String > > sources;
	protected LinkedHashMap< String, List< String > > sourceNamesAfterTransform;
	protected LinkedHashMap< String, int[] > positions;
	private ArrayList< String > gridIds;

	@Override
	public List< SourceAndConverter< T > > transform( List< SourceAndConverter< T > > sourceAndConverters )
	{
		gridIds = new ArrayList<>( sources.keySet() );

		// Make a copy because not all sources in the input list may be transformed
		CopyOnWriteArrayList< SourceAndConverter< T > > transformedSources = new CopyOnWriteArrayList<>( sourceAndConverters );

		if ( positions == null )
		{
			autoSetPositions();
		}

		double[] spacings = getSpacings( sourceAndConverters, 0.1 );

		transformSources( sourceAndConverters, transformedSources, spacings );

		return transformedSources;
	}

	/**
	 * Compute the union of the intervals covered by all Sources
	 * at the first grid position and multiply this by
	 * the spacing factor to determine the grid spacing.
	 *
	 * @param sourceAndConverters
	 * @param spacingFactor
	 * @return
	 */
	private double[] getSpacings( List< SourceAndConverter< T > > sourceAndConverters, double spacingFactor )
	{
		final ArrayList< SourceAndConverter< ? > > referenceSources = getReferenceSources( sourceAndConverters );

		final RealInterval bounds = TransformHelper.unionRealInterval( referenceSources.stream().map( sac -> sac.getSpimSource() ).collect( Collectors.toList() ) );

		double[] spacings = new double[2];
		spacings[ 0 ] = ( 1.0 + spacingFactor ) * ( bounds.realMax( 0 ) - bounds.realMin( 0 ) );
		spacings[ 1 ] = ( 1.0 + spacingFactor ) * ( bounds.realMax( 1 ) - bounds.realMin( 1 ) );
		return spacings;
	}

	private void transformSources( List< SourceAndConverter< T > > sourceAndConverters, CopyOnWriteArrayList< SourceAndConverter< T > > transformedSources, double[] spacings )
	{
		final long start = System.currentTimeMillis();
		final int nThreads = MoBIE.N_THREADS;
		final ExecutorService executorService = Executors.newFixedThreadPool( nThreads );

		for ( String gridId : sources.keySet() )
		{
			executorService.execute( () -> {
				transformSources( sourceAndConverters, transformedSources, spacings, sources.get( gridId ), getTransformedSourceNames( gridId ), positions.get( gridId ) );
			} );
		}

		executorService.shutdown();
		try {
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
		}

		System.out.println( "Transformed " + sourceAndConverters.size() + " image source(s) in " + (System.currentTimeMillis() - start) + " ms, using " + nThreads + " thread(s)." );
	}

	private List< String > getTransformedSourceNames( String gridId )
	{
		List< String > transformedSourceNames = null;
		if ( sourceNamesAfterTransform != null )
		{
			transformedSourceNames = sourceNamesAfterTransform.get( gridId );
		}
		return transformedSourceNames;
	}

	private void transformSources( List< SourceAndConverter< T > > sourceAndConverters, List< SourceAndConverter< T > > transformedSources, double[] spacings, List< String > sources, List< String > sourceNamesAfterTransform, int[] gridPosition  )
	{
		for ( String sourceName : sources )
		{
			// compute translation transform
			final AffineTransform3D affineTransform3D = new AffineTransform3D();
			affineTransform3D.translate( spacings[ 0 ] * gridPosition[ 0 ], spacings[ 1 ] * gridPosition[ 1 ], 0 );

			final SourceAndConverter< T > sourceAndConverter = Utils.getSource( sourceAndConverters, sourceName );

			if ( sourceAndConverter == null )
			{
				// This is OK, because the field `List< List< String > > sources`
				// can contain more sources than the ones that should be
				// transformed with `transform( List< SourceAndConverter< ? > > sourceAndConverters )`
				// Examples are multi-color images where there is a separate imageDisplay
				// for each color.
				continue;
			}

			AffineSourceTransformer.transform( transformedSources, affineTransform3D, sourceAndConverter, name, sourceNamesAfterTransform, sourceNameToTransform, sources );
		}
	}

	private ArrayList< SourceAndConverter< ? > > getReferenceSources( List< SourceAndConverter< T > > sourceAndConverters )
	{
		final List< String > sourceNamesAtFirstGridPosition = sources.get( gridIds.get( 0 ) );

		final ArrayList< SourceAndConverter< ? > > referenceSources = new ArrayList<>();

		for ( String name : sourceNamesAtFirstGridPosition )
		{
			final SourceAndConverter< T > source = Utils.getSource( sourceAndConverters, name );
			if ( source != null )
			{
				referenceSources.add( source );
			}
		}

		if ( referenceSources.size() == 0 )
		{
			throw new UnsupportedOperationException( "None of the sources specified at the first grid position could be found at the list of the sources that are to be transformed. Names of sources at first grid position: " + ArrayUtils.toString( sourceNamesAtFirstGridPosition ) );
		}
		else
		{
			return referenceSources;
		}

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
