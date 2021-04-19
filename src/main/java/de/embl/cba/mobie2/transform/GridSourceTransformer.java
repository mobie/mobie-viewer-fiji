package de.embl.cba.mobie2.transform;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.utils.Utils;
import net.imglib2.FinalRealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import org.apache.commons.lang.ArrayUtils;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GridSourceTransformer extends AbstractSourceTransformer
{
	private List< List< String > > sources;
	private List< int[] > positions;
	private String tableDataLocation; // containing measurements for each grid position
	private ArrayList< FinalRealInterval > intervals;

	@Override
	public List< SourceAndConverter< ? > > transform( List< SourceAndConverter< ? > > sourceAndConverters )
	{
		final ArrayList< SourceAndConverter< ? > > transformedSources = new ArrayList<>( sourceAndConverters );

		if ( positions == null )
		{
			autoSetPositions();
		}

		intervals = new ArrayList< FinalRealInterval >();

		final SourceAndConverter< ? > reference = getReferenceSource( sourceAndConverters );

		// TODO: make this work on the union of the sources
		FinalRealInterval bounds = Utils.estimateBounds( reference.getSpimSource() );
		final double spacingFactor = 0.1;
		double spacingX = ( 1.0 + spacingFactor ) * ( bounds.realMax( 0 ) - bounds.realMin( 0 ) );
		double spacingY = ( 1.0 + spacingFactor ) * ( bounds.realMax( 1 ) - bounds.realMin( 1 ) );

		for ( int i = 0; i < positions.size(); i++ )
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

				// replace original by the transformed source
				transformedSources.remove( sourceAndConverter );
				transformedSources.add( transformedSource );

				sourceNameToTransform.put( sourceName, transform3D );
				intervals.add( Utils.estimateBounds( transformedSource.getSpimSource() ) );
			}
		}

		return transformedSources;
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
