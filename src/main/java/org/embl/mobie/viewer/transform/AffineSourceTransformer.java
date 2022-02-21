package org.embl.mobie.viewer.transform;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import org.embl.mobie.viewer.playground.SourceAffineTransformer;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.Arrays;
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

	public AffineSourceTransformer( String name, double[] parameters, List< String > sources ) {
		this( name, parameters, sources, null );
	}

	public AffineSourceTransformer( String name, double[] parameters, List< String > sources, List< String > sourceNamesAfterTransform )
	{
		this.name = name;
		this.parameters = parameters;
		this.sources = sources;
		this.sourceNamesAfterTransform = sourceNamesAfterTransform;
	}

	public AffineSourceTransformer( TransformedSource< ? > transformedSource )
	{
		AffineTransform3D fixedTransform = new AffineTransform3D();
		transformedSource.getFixedTransform( fixedTransform );
		name = "manualTransform";
		parameters = fixedTransform.getRowPackedCopy();
		sources	= Arrays.asList( transformedSource.getWrappedSource().getName() );
		sourceNamesAfterTransform =	Arrays.asList( transformedSource.getName() );
	}

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
			return new SourceAffineTransformer( affineTransform3D, sourceName );
		}
	}

	@Override
	public List< String > getSources()
	{
		return sources;
	}
}
