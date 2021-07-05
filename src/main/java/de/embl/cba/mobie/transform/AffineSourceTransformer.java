package de.embl.cba.mobie.transform;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.playground.SourceAffineTransformer;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;

import java.util.ArrayList;
import java.util.List;

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

		for ( SourceAndConverter< ? > source : sourceAndConverters )
		{
			String name = source.getSpimSource().getName();
			if ( sources.contains( name ) )
			{
				SourceAffineTransformer transformer;
				if ( sourceNamesAfterTransform != null )
				{
					name = sourceNamesAfterTransform.get( sources.indexOf( name ) );
					transformer = new SourceAffineTransformer( affineTransform3D, name );
				}
				else
				{
					transformer = new SourceAffineTransformer( affineTransform3D );
				}

				final SourceAndConverter transformedSource = transformer.apply( source );
				// replace the source in the list
				transformedSources.remove( source );
				transformedSources.add( transformedSource );
				sourceNameToTransform.put( name, affineTransform3D );
			}
		}

		return transformedSources;
	}
}
