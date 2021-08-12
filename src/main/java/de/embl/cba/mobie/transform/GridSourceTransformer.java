package de.embl.cba.mobie.transform;

import bdv.util.VolatileSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.Utils;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class GridSourceTransformer extends AbstractSourceTransformer
{
	// Serialization
	protected List< String > sources;
	protected String mergedGridSourceName;
	protected List< int[] > positions;
	protected boolean centerAtOrigin = false;

	@Override
	public void transform( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter )
	{
		final List< SourceAndConverter< ? > > gridSources = getGridSources( sourceNameToSourceAndConverter );

		if ( positions == null )
			positions = createPositions( gridSources.size() );

		final SourceAndConverter< ? > mergedSourceAndConverter = createMergedSourceAndConverter( gridSources.stream().map( sac -> sac.getSpimSource() ).collect( Collectors.toList() ), gridSources.get( 0 ).asVolatile().getConverter(), gridSources.get( 0 ).getConverter() );

		sourceNameToSourceAndConverter.put( mergedSourceAndConverter.getSpimSource().getName(), mergedSourceAndConverter );

		// needed to know where the individual sources are in space:
		transformGridSourcesIndividually( sourceNameToSourceAndConverter, gridSources );
	}

	private void transformGridSourcesIndividually( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter, List< SourceAndConverter< ? > > gridSources )
	{
		final long start = System.currentTimeMillis();

		final ArrayList< SourceAndConverter< ? > > referenceSources = new ArrayList<>();
		referenceSources.add( gridSources.get( 0 ) );
		final double[] gridCellRealDimensions = TransformedGridSourceTransformer.createGridCellRealDimensions( referenceSources, TransformedGridSourceTransformer.CELL_SCALING );
		final double[] offset = TransformedGridSourceTransformer.createOffset( gridCellRealDimensions );

		final int nThreads = MoBIE.N_THREADS;
		final ExecutorService executorService = Executors.newFixedThreadPool( nThreads );

		final int numSources = gridSources.size();
		for ( int positionIndex = 0; positionIndex < numSources; positionIndex++ )
		{
			final int finalPositionIndex = positionIndex;

			final ArrayList< String > sourceNamesAtGridPosition = getSourcesAtGridPosition( gridSources, finalPositionIndex );

			// translate the source(s) at this grid position
			// (in fact, here it can only be one source per grid position)
			executorService.execute( () -> {
				TransformedGridSourceTransformer.translateToGridPosition( sourceNameToSourceAndConverter, gridCellRealDimensions, offset, sourceNamesAtGridPosition, null, positions.get( finalPositionIndex ), centerAtOrigin );
			} );

			translateSourcesWithinMergedSources( sourceNameToSourceAndConverter, gridCellRealDimensions, offset, executorService, finalPositionIndex, sourceNamesAtGridPosition );

		}
		Utils.waitUntilFinishedAndShutDown( executorService );

		System.out.println( "Transformed " + sourceNameToSourceAndConverter.size() + " image source(s) in " + (System.currentTimeMillis() - start) + " ms, using " + nThreads + " thread(s)." );
	}

	private void translateSourcesWithinMergedSources( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter, double[] gridCellRealDimensions, double[] offset, ExecutorService executorService, int finalPositionIndex, ArrayList< String > sourceNamesAtGridPosition )
	{
		final ArrayList< String > baseSourceNames = new ArrayList<>();
		for ( String sourceName : sourceNamesAtGridPosition )
		{
			final Source< ? > spimSource = sourceNameToSourceAndConverter.get( sourceName ).getSpimSource();
			if ( spimSource instanceof MergedGridSource )
			{
				baseSourceNames.addAll( ( ( MergedGridSource< ? > ) spimSource ).getGridSources().stream().map( s -> s.getName() ).collect( Collectors.toList() ) ) ;

			}
		}

		if ( baseSourceNames.size() > 0 )
		{
			executorService.execute( () -> {
				TransformedGridSourceTransformer.translateToGridPosition( sourceNameToSourceAndConverter, gridCellRealDimensions, offset, baseSourceNames, null, positions.get( finalPositionIndex ), centerAtOrigin );
			} );
		}
	}

	private ArrayList< String > getSourcesAtGridPosition( List< SourceAndConverter< ? > > gridSources, int finalPositionIndex )
	{
		final ArrayList< String > sourcesAtGridPosition = new ArrayList<>();
		sourcesAtGridPosition.add( gridSources.get( finalPositionIndex ).getSpimSource().getName() );
		return sourcesAtGridPosition;
	}

	@Override
	public List< String > getSources()
	{
		return sources;
	}

	private SourceAndConverter< ? > createMergedSourceAndConverter( List< Source< ? > > gridSources, Converter< ?, ARGBType > volatileConverter, Converter< ?, ARGBType > converter )
	{
		final MergedGridSource< ? > mergedGridSource = new MergedGridSource( gridSources, positions, mergedGridSourceName, TransformedGridSourceTransformer.CELL_SCALING );

		final VolatileSource< ?, ? > volatileMergedGridSource = new VolatileSource<>( mergedGridSource, MoBIE.sharedQueue );

		final SourceAndConverter< ? > volatileSourceAndConverter = new SourceAndConverter( volatileMergedGridSource, volatileConverter );

		final SourceAndConverter< ? > mergedSourceAndConverter = new SourceAndConverter( mergedGridSource, converter, volatileSourceAndConverter );

		return mergedSourceAndConverter;
	}

	private List< SourceAndConverter< ? > > getGridSources( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter )
	{
		final List< SourceAndConverter< ? > > gridSources = new ArrayList<>();
		for ( String sourceName : sources )
		{
			gridSources.add( sourceNameToSourceAndConverter.get( sourceName ) );
		}
		return gridSources;
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
