package org.embl.mobie.viewer.transform;

import bdv.viewer.SourceAndConverter;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.Utils;
import org.embl.mobie.viewer.playground.SourceAffineTransformer;
import net.imglib2.realtransform.AffineTransform3D;
import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransformedGridSourceTransformer extends AbstractSourceTransformer
{
	// Serialization
	protected List< List< String > > sources;
	protected List< List< String > > sourceNamesAfterTransform;
	protected List< int[] > positions;
	protected boolean centerAtOrigin = true;

	// Static
	public static final double RELATIVE_CELL_MARGIN = 0.1;

	@Override
	public void transform( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter )
	{
		if ( positions == null )
			autoSetPositions();

		final double[] cellRealDimensions = TransformHelper.getMaximalSourceUnionRealDimensions( sourceNameToSourceAndConverter, sources );

		transform( sourceNameToSourceAndConverter, cellRealDimensions );
	}

	@Override
	public List< String > getSources()
	{
		final ArrayList< String > allSources = new ArrayList<>();
		for ( List< String > sourcesAtGridPosition : sources )
			allSources.addAll( sourcesAtGridPosition );
		return allSources;
	}

	private void transform( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter, double[] cellRealDimensions )
	{
		final long start = System.currentTimeMillis();

		final int nThreads = MoBIE.N_THREADS;
		final ExecutorService executorService = Executors.newFixedThreadPool( nThreads );

		final int numGridPositions = sources.size();

		for ( int gridIndex = 0; gridIndex < numGridPositions; gridIndex++ )
		{
			int finalGridIndex = gridIndex;
			executorService.execute( () -> {
				if ( sourceNamesAfterTransform != null )
					translate( sourceNameToSourceAndConverter, sources.get( finalGridIndex ), sourceNamesAfterTransform.get( finalGridIndex ), centerAtOrigin, cellRealDimensions[ 0 ] * positions.get( finalGridIndex )[ 0 ], cellRealDimensions[ 1 ] * positions.get( finalGridIndex )[ 1 ] );
				else
					translate( sourceNameToSourceAndConverter, sources.get( finalGridIndex ), null, centerAtOrigin, cellRealDimensions[ 0 ] * positions.get( finalGridIndex )[ 0 ], cellRealDimensions[ 1 ] * positions.get( finalGridIndex )[ 1 ] );
			} );
		}

		Utils.waitUntilFinishedAndShutDown( executorService );

		System.out.println( "Transformed " + sourceNameToSourceAndConverter.size() + " image source(s) in " + (System.currentTimeMillis() - start) + " ms, using " + nThreads + " thread(s)." );
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

	private void autoSetPositions()
	{
		final int numPositions = sources.size();
		final int numX = ( int ) Math.ceil( Math.sqrt( numPositions ) );
		positions = new ArrayList<>();
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
	}
}
