package de.embl.cba.mobie2.transform;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.tables.image.SourceAndMetadata;
import net.imglib2.FinalRealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Pair;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import javax.xml.transform.Source;
import java.util.ArrayList;
import java.util.List;

public class GridSourceTransformer implements SourceTransformer
{
	private List< List< String > > sources;
	private List< int[] > positions;

	@Override
	public List< SourceAndConverter< ? > > transform( List< SourceAndConverter< ? > > sourceAndConverters )
	{
		final ArrayList< SourceAndConverter< ? > > transformedSources = new ArrayList<>( sourceAndConverters );

		if ( positions == null )
		{
			autoSetPositions();
		}

		// TODO: make this work on the union of the sources
		final SourceAndConverter< ? > reference = Utils.getSource( sourceAndConverters, sources.get( 0 ).get( 0 ) );
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
				final SourceAndConverter< ? > transformedSource = new SourceAffineTransformer( sourceAndConverter, transform3D ).getSourceOut();

				// replace original by the transformed source
				transformedSources.remove( sourceAndConverter );
				transformedSources.add( transformedSource );
			}
		}

		return transformedSources;
	}

	private void autoSetPositions()
	{
		final int numPositions = sources.size();
		final int numColumns = ( int ) Math.ceil( Math.sqrt( numPositions ) );
		positions = new ArrayList<>();
		int columnIndex = 0;
		int rowIndex = 0;
		for ( int i = 0; i < numPositions; i++ )
		{
			if ( ++columnIndex == numColumns )
			{
				rowIndex++;
			}
			positions.add( new int[]{ rowIndex, columnIndex }  );
		}
	}
}
