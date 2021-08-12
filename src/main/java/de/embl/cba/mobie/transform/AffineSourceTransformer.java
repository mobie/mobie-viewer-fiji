package de.embl.cba.mobie.transform;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.playground.SourceAffineTransformer;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;

import java.util.List;
import java.util.Map;

public class AffineSourceTransformer< T extends NumericType< T > > extends AbstractSourceTransformer< T >
{
	// Serialisation
	protected double[] parameters;
	protected List< String > sources;
	protected List< String > sourceNamesAfterTransform;

	// Runtime
	private AffineTransform3D affineTransform3D;

	@Override
	public void transform( Map< String, SourceAndConverter< T > > sourceNameToSourceAndConverter )
	{
		affineTransform3D = new AffineTransform3D();
		affineTransform3D.set( parameters );

		for ( String sourceName : sourceNameToSourceAndConverter.keySet() )
		{
			if ( sources.contains( sourceName ) )
			{
				SourceAffineTransformer transformer = createSourceAffineTransformer( sourceName );

				final SourceAndConverter transformedSource = transformer.apply( sourceNameToSourceAndConverter.get( sourceName ) );

				sourceNameToSourceAndConverter.put( transformedSource.getSpimSource().getName(), transformedSource );
			}
		}
	}

	private SourceAffineTransformer createSourceAffineTransformer( String sourceName )
	{
		if ( sourceNamesAfterTransform != null )
		{
			return new SourceAffineTransformer( affineTransform3D, sourceNamesAfterTransform.get( sources.indexOf( sourceName ) ) );
		}
		else
		{
			return new SourceAffineTransformer( affineTransform3D );
		}
	}

	@Override
	public List< String > getSources()
	{
		return sources;
	}
}
