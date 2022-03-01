package org.embl.mobie.viewer.transform;

import bdv.util.Affine3DHelpers;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.Logger;
import net.imglib2.util.LinAlgHelpers;
import org.embl.mobie.viewer.ThreadUtils;
import org.embl.mobie.viewer.playground.SourceAffineTransformer;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
		Logger.info("Transforming " + sources.size() + " sources into a grid...");
		if ( positions == null )
			autoSetPositions();

		final double[] cellRealDimensions = TransformHelpers.getMaximalSourceUnionRealDimensions( sourceNameToSourceAndConverter, sources );

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
		final int numGridPositions = sources.size();

		final ArrayList< Future< ? > > futures = ThreadUtils.getFutures();
		for ( int gridIndex = 0; gridIndex < numGridPositions; gridIndex++ )
		{
			int finalGridIndex = gridIndex;
			futures.add( ThreadUtils.executorService.submit( () -> {
				translate(
						sourceNameToSourceAndConverter,
						sources.get( finalGridIndex ),
						getSourceNamesAfterTransform( finalGridIndex ),
						centerAtOrigin,
						cellRealDimensions[ 0 ] * positions.get( finalGridIndex )[ 0 ],
						cellRealDimensions[ 1 ] * positions.get( finalGridIndex )[ 1 ] );
			} ) );
		}
		ThreadUtils.waitUntilFinished( futures );
	}

	private List< String > getSourceNamesAfterTransform( int finalGridIndex )
	{
		if ( sourceNamesAfterTransform != null )
			return sourceNamesAfterTransform.get( finalGridIndex );
		else
			return null;
	}

	public static void translate( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter, List< String > sourceNames, List< String > sourceNamesAfterTransform, boolean centerAtOrigin, double translationX, double translationY )
	{
		for ( String sourceName : sourceNames )
		{
			final SourceAndConverter< ? > sourceAndConverter = sourceNameToSourceAndConverter.get( sourceName );

			if ( sourceAndConverter == null )
			  continue;

			AffineTransform3D transform = new AffineTransform3D();

			final Source< ? > source = sourceAndConverter.getSpimSource();

			// translation
			AffineTransform3D translationTransform = TransformHelpers.createTranslationTransform3D( translationX, translationY, centerAtOrigin, source );

			//transform = translationTransform;
			transform.preConcatenate( translationTransform );

			// apply transformation
			final SourceAndConverter< ? > transformedSource = createSourceAffineTransformer( sourceName, sourceNames, sourceNamesAfterTransform, transform ).apply( sourceAndConverter );

			// store the resulting transformed source
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
