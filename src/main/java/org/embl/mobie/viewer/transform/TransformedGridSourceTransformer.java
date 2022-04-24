package org.embl.mobie.viewer.transform;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.Logger;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.ThreadUtils;
import org.embl.mobie.viewer.playground.SourceAffineTransformer;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

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
		final long startTime = System.currentTimeMillis();
		if ( positions == null )
			autoSetPositions();

		// TODO: https://github.com/mobie/mobie-viewer-fiji/issues/674
		final double[] cellRealDimensions = TransformHelpers.getMaximalSourceUnionRealDimensions( sourceNameToSourceAndConverter, sources );

		transform( sourceNameToSourceAndConverter, cellRealDimensions );
		final long duration = System.currentTimeMillis() - startTime;
		//if ( duration > MoBIE.minLogTimeMillis )
			Logger.info("Transformed " + sources.size() + " group(s) with "+ sources.get( 0 ).size() +" source(s) each into a grid in " + duration + "ms (centerAtOrigin="+centerAtOrigin+").");
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
		final int numGridPositions = sources.size();

		final ArrayList< Future< ? > > futures = ThreadUtils.getFutures();
		for ( int gridIndex = 0; gridIndex < numGridPositions; gridIndex++ )
		{
			int finalGridIndex = gridIndex;
			futures.add( ThreadUtils.executorService.submit( () -> {
				if ( sourceNamesAfterTransform != null )
					translate( sourceNameToSourceAndConverter, sources.get( finalGridIndex ), sourceNamesAfterTransform.get( finalGridIndex ), centerAtOrigin, cellRealDimensions[ 0 ] * positions.get( finalGridIndex )[ 0 ], cellRealDimensions[ 1 ] * positions.get( finalGridIndex )[ 1 ] );
				else
					translate( sourceNameToSourceAndConverter, sources.get( finalGridIndex ), null, centerAtOrigin, cellRealDimensions[ 0 ] * positions.get( finalGridIndex )[ 0 ], cellRealDimensions[ 1 ] * positions.get( finalGridIndex )[ 1 ] );
			} ) );
		}
		ThreadUtils.waitUntilFinished( futures );
	}

	public static void translate( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter, List< String > sourceNames, List< String > sourceNamesAfterTransform, boolean centerAtOrigin, double translationX, double translationY )
	{
		for ( String sourceName : sourceNames )
		{
			final SourceAndConverter< ? > sourceAndConverter = sourceNameToSourceAndConverter.get( sourceName );

			if ( sourceAndConverter == null )
			  continue;

			AffineTransform3D translationTransform = TransformHelpers.createTranslationTransform3D( translationX, translationY, sourceAndConverter, centerAtOrigin );

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
