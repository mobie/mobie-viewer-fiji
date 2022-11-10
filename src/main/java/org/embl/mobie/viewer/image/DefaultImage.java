package org.embl.mobie.viewer.image;

import bdv.VolatileSpimSource;
import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Source;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import org.embl.mobie.viewer.source.SourcePair;

import javax.annotation.Nullable;

public class DefaultImage< T > implements Image< T >
{
	private final String name;
	private final SourcePair< T > sourcePair;
	private final TransformedSource< T > transformedSource;
	private AffineTransform3D affineTransform3D;
	private RealMaskRealInterval mask;

	public DefaultImage( String name, SourcePair< T > sourcePair, @Nullable RealMaskRealInterval mask )
	{
		this.name = name;
		this.mask = mask;

		// Wrap into a transformed source to allow additional
		// transformations.
		affineTransform3D = new AffineTransform3D();
		transformedSource = new TransformedSource( sourcePair.getSource() );
		final TransformedSource volatileTransformedSource = new TransformedSource( sourcePair.getVolatileSource(), transformedSource );
		this.sourcePair = new DefaultSourcePair( transformedSource, volatileTransformedSource );
	}

	@Override
	public SourcePair< T > getSourcePair()
	{
		return sourcePair;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public void transform( AffineTransform3D affineTransform3D )
	{
		if ( mask != null )
		{
			// The mask contains potential previous transforms already,
			// thus we add the new transform on top.
			mask = mask.transform( affineTransform3D.inverse() );
		}

		this.affineTransform3D.preConcatenate( affineTransform3D );
		transformedSource.setFixedTransform( this.affineTransform3D );
	}

	@Override
	public RealMaskRealInterval getMask()
	{
		return mask;
	}

	@Override
	public void setMask( RealMaskRealInterval mask )
	{
		this.mask = mask;
	}
}
