package org.embl.mobie.viewer.transform;

import bdv.viewer.SourceAndConverter;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.Utils;
import org.embl.mobie.viewer.playground.SourceAffineTransformer;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class TransformedGridSourceTransformer extends AbstractSourceTransformer
{
	public static final double RELATIVE_CELL_MARGIN = 0.1;

	// Serialization
	protected LinkedHashMap< String, List< String > > sources;
	protected LinkedHashMap< String, List< String > > sourceNamesAfterTransform;
	protected LinkedHashMap< String, int[] > positions;
	protected boolean centerAtOrigin = false;

	// Runtime
	private transient ArrayList< String > gridIds;

	@Override
	public void transform( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter )
	{
		gridIds = new ArrayList<>( sources.keySet() );

		if ( positions == null )
			autoSetPositions();

		final List< SourceAndConverter< ? > > referenceSources = getReferenceSources( sourceNameToSourceAndConverter );

		transform( sourceNameToSourceAndConverter, referenceSources );
	}

	@Override
	public List< String > getSources()
	{
		final ArrayList< String > allSources = new ArrayList<>();
		for ( List< String > sources : this.sources.values() )
			allSources.addAll( sources );
		return allSources;
	}

	private void transform( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter, List< SourceAndConverter< ? > > referenceSources )
	{
		final double[] cellRealDimensions = computeGridCellRealDimensions( referenceSources, RELATIVE_CELL_MARGIN );

		final long start = System.currentTimeMillis();

		final int nThreads = MoBIE.N_THREADS;
		final ExecutorService executorService = Executors.newFixedThreadPool( nThreads );

		for ( String gridId : sources.keySet() )
		{
			executorService.execute( () -> {
				translate( sourceNameToSourceAndConverter, sources.get( gridId ), sourceNamesAfterTransform.get( gridId ), centerAtOrigin, cellRealDimensions[ 0 ] * positions.get( gridId )[ 0 ], cellRealDimensions[ 1 ] * positions.get( gridId )[ 1 ] );
			} );
		}

		Utils.waitUntilFinishedAndShutDown( executorService );

		System.out.println( "Transformed " + sourceNameToSourceAndConverter.size() + " image source(s) in " + (System.currentTimeMillis() - start) + " ms, using " + nThreads + " thread(s)." );
	}

	public static double[] computeGridCellRealDimensions( List< SourceAndConverter< ? > > sources, double relativeCellMargin )
	{
		RealInterval bounds = TransformHelper.unionRealInterval( sources.stream().map( sac -> sac.getSpimSource() ).collect( Collectors.toList() ));
		final double[] cellDimensions = new double[ 2 ];
		for ( int d = 0; d < 2; d++ )
			cellDimensions[ d ] = ( 1.0 + 2.0 * relativeCellMargin ) * ( bounds.realMax( d ) - bounds.realMin( d ) );
		return cellDimensions;
	}

	public static void translate( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter, List< String > sourceNames, List< String > sourceNamesAfterTransform, boolean centerAtOrigin, double translationX, double translationY )
	{
		for ( String sourceName : sourceNames )
		{
			final SourceAndConverter< ? > sourceAndConverter = sourceNameToSourceAndConverter.get( sourceName );

			if ( sourceAndConverter == null )
			  continue;

			AffineTransform3D translationTransform = TransformHelper.createTranslationTransform3D( translationX, translationY, sourceAndConverter, centerAtOrigin );

			final SourceAffineTransformer transformer = createSourceAffineTransformer( sourceName, sourceNames, sourceNamesAfterTransform, translationTransform );

			final SourceAndConverter transformedSource = transformer.apply( sourceNameToSourceAndConverter.get( sourceName ) );

			sourceNameToSourceAndConverter.put( transformedSource.getSpimSource().getName(), transformedSource );
		}
	}

	public static SourceAffineTransformer createSourceAffineTransformer( String sourceName, List< String > sourceNames, List< String > sourceNamesAfterTransform, AffineTransform3D affineTransform3D )
	{
		if ( sourceNamesAfterTransform != null )
		{
			return new SourceAffineTransformer( affineTransform3D, sourceNamesAfterTransform.get( sourceNames.indexOf( sourceName ) ) );
		}
		else
		{
			return new SourceAffineTransformer( affineTransform3D );
		}
	}

	private List< SourceAndConverter< ? > > getReferenceSources( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter )
	{
		final List< String > sourceNamesAtFirstGridPosition = sources.get( gridIds.get( 0 ) );

		List< SourceAndConverter< ? > > referenceSources = new ArrayList<>();
		for ( String sourceName : sourceNamesAtFirstGridPosition )
		{
			final SourceAndConverter< ? > sourceAndConverter = sourceNameToSourceAndConverter.get( sourceName );
			if ( sourceAndConverter != null )
			{
				referenceSources.add( sourceAndConverter );
			}
		}

		if ( referenceSources.size() != 0 )
		{
			return referenceSources;
		}
		else
		{
			throw new UnsupportedOperationException( "None of the sources specified at the first grid position could not be found at the list of the sources that are to be transformed. Names of sources at first grid position: " + ArrayUtils.toString( sourceNamesAtFirstGridPosition ) );
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
