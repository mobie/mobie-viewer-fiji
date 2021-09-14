package org.embl.mobie.viewer.transform;

import bdv.viewer.SourceAndConverter;
import org.embl.mobie.viewer.playground.SourceAffineTransformer;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.List;
import java.util.Map;

public class AffineSourceTransformer extends AbstractSourceTransformer
{
	// Serialisation
	protected double[] parameters;
	protected List< String > sources;
	protected List< String > sourceNamesAfterTransform;

	// Runtime
	private transient AffineTransform3D affineTransform3D;

	@Override
	public void transform( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter )
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
