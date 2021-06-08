package de.embl.cba.mobie.transform;

import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.util.ArrayList;
import java.util.List;

public class AffineSourceTransformer< T extends NumericType< T > > extends AbstractSourceTransformer< T >
{
	private double[] parameters;

	@Override
	public List< SourceAndConverter< T > > transform( List< SourceAndConverter< T > > sources )
	{
		final ArrayList< SourceAndConverter< T > > transformedSources = new ArrayList<>( sources );

		final AffineTransform3D affineTransform3D = new AffineTransform3D();
		affineTransform3D.set( parameters );
		final SourceAffineTransformer transformer = new SourceAffineTransformer( affineTransform3D );

		for ( SourceAndConverter< ? > source : sources )
		{
			final String name = source.getSpimSource().getName();
			if ( this.sources.contains( name ) )
			{
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
