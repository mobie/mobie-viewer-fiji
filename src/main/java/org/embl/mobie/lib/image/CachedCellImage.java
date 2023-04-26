package org.embl.mobie.lib.image;

import bdv.cache.SharedQueue;
import bdv.tools.transformation.TransformedSource;
import bdv.util.RandomAccessibleIntervalSource;
import bdv.util.RandomAccessibleIntervalSource4D;
import bdv.viewer.Source;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.Util;
import org.embl.mobie.io.CachedCellImgOpener;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.lib.source.SourceHelper;

import javax.annotation.Nullable;

public class CachedCellImage< T > implements Image< T >
{
	private final String path;
	private final Integer channel;
	private final String name;
	private final ImageDataFormat imageDataFormat;
	@Nullable private final SharedQueue sharedQueue;
	private SourcePair< T > sourcePair;
	private TransformedSource< T > transformedSource;
	private AffineTransform3D affineTransform3D;
	private RealMaskRealInterval mask;

	public CachedCellImage( String name, String path, Integer channel, ImageDataFormat imageDataFormat, @Nullable SharedQueue sharedQueue )
	{
		this.path = path;
		this.channel = channel;
		this.name = name;
		this.imageDataFormat = imageDataFormat;
		this.sharedQueue = sharedQueue;
		this.affineTransform3D = new AffineTransform3D();
	}

	@Override
	public SourcePair< T > getSourcePair()
	{
		if( sourcePair == null ) open();
		return sourcePair;
	}

	private void open()
	{
		final CachedCellImgOpener< ? > opener = new CachedCellImgOpener<>( path, imageDataFormat, sharedQueue );

		Source< T > source = getSource( opener );
		Source< ? extends Volatile< T > > volatileSource = getVolatileSource( opener );

		transformedSource = new TransformedSource( source );
		transformedSource.setFixedTransform( affineTransform3D );
		sourcePair = new DefaultSourcePair( transformedSource, new TransformedSource( volatileSource, transformedSource ) );
	}

	private Source< T > getSource( CachedCellImgOpener< ? > opener )
	{
		final RandomAccessibleInterval< ? > rai = opener.getRAI( channel );
		Source< ? > source = asSource( rai );
		return ( Source< T > ) source;
	}

	private Source< ? extends Volatile< T > > getVolatileSource( CachedCellImgOpener< ? > opener )
	{
		final RandomAccessibleInterval< ? > rai = opener.getVolatileRAI( channel );
		Source< ? > source = asSource( rai );
		return ( Source< ? extends Volatile< T > > ) source;
	}

	private Source< ? > asSource( RandomAccessibleInterval< ? > rai )
	{
		Source< ? > source;
		if ( rai.numDimensions() > 3 )
		{
			source = new RandomAccessibleIntervalSource4D(
					rai,
					( NumericType ) Util.getTypeFromInterval( rai ),
					affineTransform3D,
					name );
		} else
		{
			source = new RandomAccessibleIntervalSource(
					rai,
					( NumericType ) Util.getTypeFromInterval( rai ),
					affineTransform3D,
					name );
		}

		// no spatial calibration
		SourceHelper.setVoxelDimensionsToPixels( source );

		return source;
	}


	@Override
	public String getName()
	{
		return name;
	}

	@Override // TODO code duplication with SpimDataImage
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
}
