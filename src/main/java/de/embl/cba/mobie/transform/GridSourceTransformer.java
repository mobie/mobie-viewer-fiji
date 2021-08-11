package de.embl.cba.mobie.transform;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.Utils;
import de.embl.cba.mobie.playground.SourceAffineTransformer;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class GridSourceTransformer< T extends NumericType< T > > extends AbstractSourceTransformer< T >
{
	// Serialization
	protected LinkedHashMap< String, List< String > > sources;
	protected LinkedHashMap< String, List< String > > sourceNamesAfterTransform;
	protected LinkedHashMap< String, int[] > positions;
	protected boolean centerAtOrigin = false;

	private ArrayList< String > gridIds;

	@Override
	public void transform( Map< String, SourceAndConverter< T > > sourceNameToSourceAndConverter )
	{
		gridIds = new ArrayList<>( sources.keySet() );

		if ( positions == null )
			autoSetPositions();

		final List< SourceAndConverter< T > > referenceSources = getReferenceSources( sourceNameToSourceAndConverter );

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

	private void transform( Map< String, SourceAndConverter< T > > sourceNameToSourceAndConverter, List< SourceAndConverter< T > > referenceSources )
	{
		RealInterval bounds = TransformHelper.unionRealInterval(  referenceSources.stream().map( sac -> sac.getSpimSource() ).collect( Collectors.toList() ));
		final double spacingFactor = 0.1;
		double spacingX = ( 1.0 + spacingFactor ) * ( bounds.realMax( 0 ) - bounds.realMin( 0 ) );
		double spacingY = ( 1.0 + spacingFactor ) * ( bounds.realMax( 1 ) - bounds.realMin( 1 ) );

		final long start = System.currentTimeMillis();

		final int nThreads = MoBIE.N_THREADS;
		final ExecutorService executorService = Executors.newFixedThreadPool( nThreads );

		for ( String gridId : sources.keySet() )
		{
			executorService.execute( () -> {
				transform( sourceNameToSourceAndConverter, spacingX, spacingY, sources.get( gridId ), sourceNamesAfterTransform.get( gridId ), positions.get( gridId ) );
			} );
		}

		Utils.waitUntilFinishedAndShutDown( executorService );

		System.out.println( "Transformed " + sourceNameToSourceAndConverter.size() + " image source(s) in " + (System.currentTimeMillis() - start) + " ms, using " + nThreads + " thread(s)." );
	}

	private void transform( Map< String, SourceAndConverter< T > > sourceNameToSourceAndConverter, double spacingX, double spacingY, List< String > sourceNames, List< String > sourceNamesAfterTransform, int[] gridPosition  )
	{
		for ( String sourceName : sourceNames )
		{
			final SourceAndConverter< T > sourceAndConverter = sourceNameToSourceAndConverter.get( sourceName );

			if ( sourceAndConverter == null )
			  continue;

			AffineTransform3D translationTransform = TransformHelper.createTranslationTransform3D( spacingX * gridPosition[ 0 ], spacingY * gridPosition[ 1 ], sourceAndConverter, centerAtOrigin );

			final SourceAffineTransformer transformer = createSourceAffineTransformer( sourceName, sourceNames, sourceNamesAfterTransform, translationTransform );

			final SourceAndConverter transformedSource = transformer.apply( sourceNameToSourceAndConverter.get( sourceName ) );

			sourceNameToSourceAndConverter.put( transformedSource.getSpimSource().getName(), transformedSource );
		}
	}

	private SourceAffineTransformer createSourceAffineTransformer( String sourceName, List< String > sourceNames, List< String > sourceNamesAfterTransform, AffineTransform3D affineTransform3D )
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

	private List< SourceAndConverter< T > > getReferenceSources( Map< String, SourceAndConverter< T > > sourceNameToSourceAndConverter )
	{
		final List< String > sourceNamesAtFirstGridPosition = sources.get( gridIds.get( 0 ) );

		List< SourceAndConverter< T  > > referenceSources = new ArrayList<>();
		for ( String sourceName : sourceNamesAtFirstGridPosition )
		{
			final SourceAndConverter< T > sourceAndConverter = sourceNameToSourceAndConverter.get( sourceName );
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
