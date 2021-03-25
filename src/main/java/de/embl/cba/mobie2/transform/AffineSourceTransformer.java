package de.embl.cba.mobie2.transform;

import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import javax.xml.transform.Source;
import java.util.ArrayList;
import java.util.List;

public class AffineSourceTransformer implements SourceTransformer
{
	private List< String > sources;
	private double[] parameters;

	@Override
	public List< SourceAndConverter< ? > > transform( List< SourceAndConverter< ? > > sources )
	{
		final ArrayList< SourceAndConverter< ? > > transformedSources = new ArrayList<>( sources );

		final AffineTransform3D affineTransform3D = new AffineTransform3D();
		affineTransform3D.set( parameters );
		final SourceAffineTransformer transformer = new SourceAffineTransformer( affineTransform3D );

		for ( SourceAndConverter< ? > source : sources )
		{
			if ( this.sources.contains( source.getSpimSource().getName() ) )
			{
				final SourceAndConverter transformedSource = transformer.apply( source );
				// replace the source in the list
				transformedSources.remove( source );
				transformedSources.add( transformedSource );
			}
		}

		return transformedSources;
	}
}
