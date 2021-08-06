package de.embl.cba.mobie.transform;

import bdv.viewer.Source;
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
			autoSetPositions();
		}

		final List< Source< T > > gridSources = sources.stream().map( sourceName -> Utils.getSource( sourceAndConverters, sourceName ).getSpimSource() ).collect( Collectors.toList() );

		new MergedGridSource<>( gridSources, positions, mergedGridSourceName );

		List< SourceAndConverter< T > > transformedSourceAndConverters = new CopyOnWriteArrayList<>( sourceAndConverters );



		createMergedGridSource( sourceAndConverters, transformedSourceAndConverters, referenceSource );

		return transformedSourceAndConverters;
	}

	private void createMergedGridSource( List< SourceAndConverter< T > > inputSources, List< SourceAndConverter< T > > transformedSources, SourceAndConverter< T > referenceSources )
	{


		final double spacingFactor = 0.1;
		double spacingX = ( 1.0 + spacingFactor ) * ( bounds.realMax( 0 ) - bounds.realMin( 0 ) );
		double spacingY = ( 1.0 + spacingFactor ) * ( bounds.realMax( 1 ) - bounds.realMin( 1 ) );

		final long start = System.currentTimeMillis();

		final int nThreads = MoBIE.N_THREADS;
		final ExecutorService executorService = Executors.newFixedThreadPool( nThreads );
		//final ExecutorService executorService = MoBIE.executorService;

		for ( String gridId : sources.keySet() )
		{
			executorService.execute( () -> {
				transform( inputSources, transformedSources, spacingX, spacingY, sources.get( gridId ), getTransformedSourceNames( gridId ), positions.get( gridId ) );
			} );
		}

		Utils.waitUntilFinishedAndShutDown( executorService );

		System.out.println( "Transformed " + inputSources.size() + " image source(s) in " + (System.currentTimeMillis() - start) + " ms, using " + nThreads + " thread(s)." );
	}

	private void transform( List< SourceAndConverter< T > > inputSources, List< SourceAndConverter< T > > transformedSources, List< SourceAndConverter< T > > referenceSources )
	{
		RealInterval bounds = TransformHelper.unionRealInterval(  referenceSources.stream().map( sac -> sac.getSpimSource() ).collect( Collectors.toList() ));
		final double spacingFactor = 0.1;
		double spacingX = ( 1.0 + spacingFactor ) * ( bounds.realMax( 0 ) - bounds.realMin( 0 ) );
		double spacingY = ( 1.0 + spacingFactor ) * ( bounds.realMax( 1 ) - bounds.realMin( 1 ) );

		final long start = System.currentTimeMillis();

		final int nThreads = MoBIE.N_THREADS;
		final ExecutorService executorService = Executors.newFixedThreadPool( nThreads );
		//final ExecutorService executorService = MoBIE.executorService;

		for ( String gridId : sources.keySet() )
		{
			executorService.execute( () -> {
				transform( inputSources, transformedSources, spacingX, spacingY, sources.get( gridId ), getTransformedSourceNames( gridId ), positions.get( gridId ) );
			} );
		}

		Utils.waitUntilFinishedAndShutDown( executorService );

		System.out.println( "Transformed " + inputSources.size() + " image source(s) in " + (System.currentTimeMillis() - start) + " ms, using " + nThreads + " thread(s)." );
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

	private void transform( List< SourceAndConverter< T > > sourceAndConverters, List< SourceAndConverter< T > > transformedSources, double spacingX, double spacingY, List< String > sourceNames, List< String > sourceNamesAfterTransform, int[] gridPosition  )
	{
		for ( String sourceName : sourceNames )
		{
			SourceAndConverter< T > sourceAndConverter = Utils.getSource( sourceAndConverters, sourceName );

			if ( sourceAndConverter == null )
			{
				// This is OK, because the field `List< List< String > > sources`
				// can contain more sources than the ones that should be
				// transformed with `transform( List< SourceAndConverter< ? > > sourceAndConverters )`
				// Examples are multi-color images where there is a separate imageDisplay
				// for each color.
				continue;
			}

			// compute translation transform
			AffineTransform3D translationTransform = createTranslationTransform3D( spacingX * gridPosition[ 0 ], spacingY * gridPosition[ 1 ], sourceAndConverter, centerAtOrigin );

			// apply translation transform
			AffineSourceTransformer.transform( transformedSources, translationTransform, sourceAndConverter, sourceName, sourceNamesAfterTransform, sourceNameToTransform, sourceNames );
		}
	}

	private AffineTransform3D createTranslationTransform3D( double x, double y, SourceAndConverter< T > sourceAndConverter, boolean centerAtOrigin )
	{
		AffineTransform3D translationTransform = new AffineTransform3D();
		if ( centerAtOrigin )
		{
			final double[] center = TransformHelper.getCenter( sourceAndConverter );
			translationTransform.translate( center );
			translationTransform = translationTransform.inverse();
		}
		translationTransform.translate( x, y, 0 );
		return translationTransform;
	}

	private SourceAndConverter< T > getReferenceSource( List< SourceAndConverter< T > > sourceAndConverters )
	{
		final String sourceNameAtFirstGridPosition = sources.get( 0 );
		final SourceAndConverter< T > source = Utils.getSource( sourceAndConverters, sourceNameAtFirstGridPosition );
		if ( source != null )
		{
			return source;
		}
		else
		{
			throw new UnsupportedOperationException( "The sources specified at the first grid position could not be found at the list of the sources that are to be transformed. Name of source at first grid position: " + sourceNameAtFirstGridPosition );
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
