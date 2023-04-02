package org.embl.mobie.lib.image;

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
import org.embl.mobie.lib.hcs.Site;
import org.embl.mobie.lib.hcs.SiteSpimDataCreator;
import org.embl.mobie.lib.source.SourceHelper;

import javax.annotation.Nullable;

public class SpimDataImage< T extends NumericType< T > & RealType< T > > implements Image< T >
{
	private ImageDataFormat imageDataFormat;
	private String path;
	private int setupId;
	private SourcePair< T > sourcePair;
	private String name;
	private Site site;
	private SharedQueue sharedQueue;
	private Boolean removeSpatialCalibration = false;
	@Nullable
	private RealMaskRealInterval mask;
	private TransformedSource transformedSource;
	private AffineTransform3D affineTransform3D = new AffineTransform3D();

	public SpimDataImage( AbstractSpimData< ? > spimData, int channel, String name, Boolean removeSpatialCalibration  )
	{
		this.imageDataFormat = null;
		this.path = null;
		this.sharedQueue = null;
		this.setupId = channel;
		this.name = name;
		this.removeSpatialCalibration = removeSpatialCalibration;
		createSourcePair( spimData, channel, name );
	}

	public SpimDataImage( ImageDataFormat imageDataFormat, String path, int setupId, String name, @Nullable SharedQueue sharedQueue, Boolean removeSpatialCalibration )
	{
		this.imageDataFormat = imageDataFormat;
		this.path = path;
		this.setupId = setupId;
		this.name = name;
		this.sharedQueue = sharedQueue;
		this.removeSpatialCalibration = removeSpatialCalibration;
	}

	public SpimDataImage( Site site, String name )
	{
		this.setupId = site.channel;
		this.name = name;
		this.site = site;
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
		final AbstractSpimData spimData = tryOpenSpimData();

		createSourcePair( spimData, setupId, name );
	}

	private void createSourcePair( AbstractSpimData spimData, int setupId, String name )
	{
		final SpimSource< T > source = new SpimSource<>( spimData, setupId, name );
		final VolatileSpimSource< ? extends Volatile< T > > vSource = new VolatileSpimSource<>( spimData, setupId, name );

		if ( removeSpatialCalibration )
		{
			source.getSourceTransform( 0, 0, affineTransform3D );
			affineTransform3D = affineTransform3D.inverse();
			SourceHelper.setVoxelDimensionsToPixels( source );
			SourceHelper.setVoxelDimensionsToPixels( vSource );
		}

		transformedSource = new TransformedSource( source );
		transformedSource.setFixedTransform( affineTransform3D );

		sourcePair = new DefaultSourcePair( transformedSource, new TransformedSource( vSource, transformedSource ) );
	}

	private AbstractSpimData tryOpenSpimData( )
	{
		try
		{
			if ( site != null )
			{
				return SiteSpimDataCreator.create( site );
			}

			return new SpimDataOpener().open( path, imageDataFormat, sharedQueue );
		}
		catch ( SpimDataException e )
		{
			throw new RuntimeException( e );
		}
	}

}
