package de.embl.cba.mobie.transform;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.playground.SourceAffineTransformer;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AffineSourceTransformer< T extends NumericType< T > > extends AbstractSourceTransformer< T >
{
	// Serialisation
	protected double[] parameters;
	protected List< String > sources;
	protected List< String > sourceNamesAfterTransform;

	@Override
	public List< SourceAndConverter< T > > transform( List< SourceAndConverter< T > > sourceAndConverters )
	{
		final ArrayList< SourceAndConverter< T > > transformedSources = new ArrayList<>( sourceAndConverters );

		final AffineTransform3D affineTransform3D = new AffineTransform3D();
		affineTransform3D.set( parameters );

		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			String sourceName = sourceAndConverter.getSpimSource().getName();
			if ( sources.contains( sourceName ) )
			{
				transform( transformedSources, affineTransform3D, sourceAndConverter, sourceName, sourceNamesAfterTransform, sourceNameToTransform, sources );
			}
		}

		return transformedSources;
	}

	// TODO: can this be simplified?
	// Note: this is also used by the GridSourceTransformer
	public static < T extends NumericType< T > > SourceAndConverter transform( List< SourceAndConverter< T > > transformedSources, AffineTransform3D affineTransform3D, SourceAndConverter< ? > source, String sourceName, List< String > sourceNamesAfterTransform, Map< String, AffineTransform3D > sourceNameToTransform, List< String > sourceNames )
	{
		SourceAffineTransformer transformer;
		if ( sourceNamesAfterTransform != null )
		{
			sourceName = sourceNamesAfterTransform.get( sourceNames.indexOf( sourceName ) );
			transformer = new SourceAffineTransformer( affineTransform3D, sourceName );
		}
		else
		{
			transformer = new SourceAffineTransformer( affineTransform3D );
		}

		final SourceAndConverter transformedSource = transformer.apply( source );

		// replace the source in the list
		transformedSources.remove( source );
		transformedSources.add( transformedSource );

		sourceNameToTransform.put( sourceName, affineTransform3D );

		return transformedSource;
	}
}
