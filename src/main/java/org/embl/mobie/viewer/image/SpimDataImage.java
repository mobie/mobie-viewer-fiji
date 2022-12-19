package org.embl.mobie.viewer.image;

import bdv.SpimSource;
import bdv.VolatileSpimSource;
import bdv.tools.transformation.TransformedSource;
import bdv.cache.SharedQueue;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.SpimDataOpener;
import org.embl.mobie.viewer.source.SourceHelper;
import org.embl.mobie.viewer.source.SourcePair;

import javax.annotation.Nullable;

public class SpimDataImage< T extends NumericType< T > & RealType< T > > implements Image< T >
{
	private final ImageDataFormat imageDataFormat;
	private final String path;
	private final int setupId;
	private SourcePair< T > sourcePair;
	private String name;
	private final SharedQueue sharedQueue; @Nullable
	private RealMaskRealInterval mask;
	private TransformedSource transformedSource;
	private AffineTransform3D affineTransform3D = new AffineTransform3D();

	public SpimDataImage( AbstractSpimData< ? > spimData, int setupId, String name, @Nullable SharedQueue sharedQueue )
	{
		this.imageDataFormat = null;
		this.path = null;
		this.setupId = setupId;
		this.sharedQueue = sharedQueue;
		createSourcePair( spimData, setupId, name );
	}

	public SpimDataImage( ImageDataFormat imageDataFormat, String path, int setupId, String name, @Nullable SharedQueue sharedQueue )
	{
		this.imageDataFormat = imageDataFormat;
		this.path = path;
		this.setupId = setupId;
		this.name = name;
		this.sharedQueue = sharedQueue;
	}

	@Override
	public SourcePair< T > getSourcePair()
	{
		if( sourcePair == null ) open();
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

		if ( transformedSource != null )
			transformedSource.setFixedTransform( this.affineTransform3D );

		for ( ImageListener listener : listeners.list )
			listener.imageChanged();
	}

	@Override
	public RealMaskRealInterval getMask( )
	{
		if ( mask == null )
		{
			// It is important to include the voxel dimensions,
			// because otherwise rendering 2D sources in a 3D scene
			// will make them so thin that the {@code RegionLabelImage}
			// does not render anything.
			return SourceHelper.estimateMask( getSourcePair().getSource(), 0, true );
		}

		return mask;
	}

	@Override
	public void setMask( RealMaskRealInterval mask )
	{
		this.mask = mask;
	}

	private void open()
	{
		final AbstractSpimData spimData = tryOpenSpimData( path, imageDataFormat, sharedQueue );

		createSourcePair( spimData, setupId, name );
	}

	private void createSourcePair( AbstractSpimData spimData, int setupId, String name )
	{
		final SpimSource< T > s = new SpimSource<>( spimData, setupId, name );
		transformedSource = new TransformedSource( s );
		transformedSource.setFixedTransform( affineTransform3D );

		final VolatileSpimSource< ? extends Volatile< T > > vs = new VolatileSpimSource<>( spimData, setupId, name );
		final TransformedSource volatileTransformedSource = new TransformedSource( vs, transformedSource );

		sourcePair = new DefaultSourcePair( transformedSource, volatileTransformedSource );
	}

	public static AbstractSpimData tryOpenSpimData( String path, ImageDataFormat imageDataFormat, SharedQueue sharedQueue )
	{
		try
		{
			if ( sharedQueue == null )
				return new SpimDataOpener().openSpimData( path, imageDataFormat );

			return new SpimDataOpener().openSpimData( path, imageDataFormat, sharedQueue );
		}
		catch ( SpimDataException e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}
}
