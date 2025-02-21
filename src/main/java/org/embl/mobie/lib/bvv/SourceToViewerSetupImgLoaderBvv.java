package org.embl.mobie.lib.bvv;

import java.net.Inet4Address;
import java.util.Random;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealUnsignedShortConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;

import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import net.imglib2.util.Cast;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;

import bdv.AbstractViewerSetupImgLoader;
import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.cache.CacheControl;
import bdv.img.cache.CacheArrayLoader;
import bdv.img.cache.VolatileCachedCellImg;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.viewer.Source;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.source.AnnotationType;
import org.embl.mobie.lib.util.MoBIEHelper;

import javax.annotation.Nullable;

public class SourceToViewerSetupImgLoaderBvv extends AbstractViewerSetupImgLoader< UnsignedShortType, VolatileUnsignedShortType > implements ViewerImgLoader
{
	final Source<?> src;
	final int numScales;
	final AffineTransform3D [] mipmapTransforms;
	final double [][] mipmapResolutions;
	private ValuePair< Double, Double > minMax;
	private VolatileGlobalCellCache cache;
	private CacheArrayLoader< VolatileShortArray > loader;
	
	public SourceToViewerSetupImgLoaderBvv( final Source<?> source )
	{
		super( new UnsignedShortType(), new VolatileUnsignedShortType() );
		src = source;
		numScales = src.getNumMipmapLevels();
		cache = new VolatileGlobalCellCache( numScales+1, 1 );

		mipmapTransforms = new AffineTransform3D[ numScales ];
		mipmapResolutions = new double[ numScales ][];
		AffineTransform3D transformSource = new AffineTransform3D();
		src.getSourceTransform( 0, 0, transformSource );
		
		final double [] zeroScale = MoBIEHelper.getScale( transformSource);
		//double [] currMipMapRes = new double [3];
		for(int i=0;i<numScales;i++)
		{
			AffineTransform3D transform = new AffineTransform3D();
			src.getSourceTransform( 0, i, transform );			
			mipmapTransforms[i] = transform;
			
			double [] currScale = MoBIEHelper.getScale( transform );
			mipmapResolutions[i] = new double [3];

			for(int d=0;d<3;d++)
			{
				mipmapResolutions[i][d] = currScale[d]/zeroScale[d];
			}		
		}

		if ( src.getType() instanceof RealType
				&& ! ( src.getType() instanceof IntegerType ) )
		{
			minMax = getMinMax( src );
		}

		loader = new SourceArrayLoader( src, minMax );
	}

	@Override
	public int numMipmapLevels()
	{
		return numScales;
	}
	
	@Override
	public double[][] getMipmapResolutions()
	{
		return mipmapResolutions;
	}

	@Override
	public AffineTransform3D[] getMipmapTransforms()
	{
		return mipmapTransforms;
	}
	
	@SuppressWarnings( "unchecked" )
	@Override
	public RandomAccessibleInterval< UnsignedShortType > getImage( int t, int level, ImgLoaderHint... hints )
	{
		final RandomAccessibleInterval< ? > raiXYZ = src.getSource( t, level );
		
		if( src.getType() instanceof IntegerType )
		{
			return convertIntegerRAIToShort( raiXYZ );
		}
		else if ( src.getType() instanceof RealType )
		{
			return convertRealRAIToShort( raiXYZ, minMax );
		}
		else if  ( src.getType() instanceof AnnotationType )
		{
			return convertAnnotationRAIToShort( raiXYZ );
		}
		else
		{
			return null;
		}
	}
	
	protected <T extends NativeType<T>> VolatileCachedCellImg<T, VolatileShortArray>
	prepareCachedImage(final int t, final int level, final int setupId,
					   final LoadingStrategy loadingStrategy, final T typeCache)
	{
		final long[] dimensions = src.getSource( t, level ).dimensionsAsLongArray();
		final int priority = numScales - 1 - level;
		
		final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );
		
		//final int[] cellDimensions = new int [] {(int)imageDimensions[level][0],(int)imageDimensions[level][1],1};
		final int[] cellDimensions = new int [] {32,32,32};
		
		final CellGrid grid = new CellGrid(dimensions, cellDimensions);
		return cache.createImg( grid, t, setupId, level, cacheHints, loader, typeCache );
	}
	
	@Override
	public RandomAccessibleInterval< VolatileUnsignedShortType > getVolatileImage( int t, int level, ImgLoaderHint... hints )
	{		
		return prepareCachedImage(t, level, 0, LoadingStrategy.VOLATILE, volatileType );
	}

	@Override
	public CacheControl getCacheControl()
	{		
		return cache;
	}
	
	@Override
	public ViewerSetupImgLoader< ?, ? > getSetupImgLoader( int setupId )
	{
		return this;
	}
	
	public void setCache( final VolatileGlobalCellCache cache )
	{
		this.cache = cache;
	}
		
	
	static class SourceArrayLoader implements CacheArrayLoader<VolatileShortArray> 
	{
		
		final Source<?> src;
		private final ValuePair< Double, Double > minMax; // only needed for FloatType

		public SourceArrayLoader( final Source<?> src,
								  @Nullable final ValuePair< Double, Double > minMax )
		{
			this.src = src;
			this.minMax = minMax;
		}
		
		@Override
		public int getBytesPerElement() {
			return 2;
		}

		@Override
		public  VolatileShortArray loadArray( int timepoint, int setup, int level, int[] dimensions, long[] min ) throws InterruptedException
		{
			final RandomAccessibleInterval< ? > raiXYZ = src.getSource( timepoint, level );
			
			final short[] data = new short[dimensions[0]*dimensions[1]*dimensions[2]];

			final long[][] interval = new long [2][3];
			for(int d=0;d<3;d++)
			{
				interval[0][d] = min[d];
				interval[1][d] = min[d] + dimensions[d] - 1;
			}
			FinalInterval finalInterval = new FinalInterval( interval[ 0 ], interval[ 1 ] );

			IterableInterval< UnsignedShortType > rai;

			if ( src.getType() instanceof IntegerType )
			{
				rai = Views.flatIterable( convertIntegerRAIToShort (Views.interval( raiXYZ, finalInterval )));
			}
			else if ( src.getType() instanceof RealType )
			{
				rai = Views.flatIterable( convertRealRAIToShort( Views.interval( raiXYZ, finalInterval ), minMax ));
			}
			else if ( src.getType() instanceof AnnotationType )
			{
				rai = Views.flatIterable( convertAnnotationRAIToShort( Views.interval( raiXYZ, finalInterval ) ) );
			}
			else
			{
				rai = null; // FIXME: should not happen, but throw error
			}

			int nCount = 0;

			Cursor< UnsignedShortType > cur = rai.cursor();
			while (cur.hasNext())
			{
				cur.fwd();
				data[nCount] = cur.get().getShort();
				nCount++;
			}
			return new VolatileShortArray(data,true);
		}
	}
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static RandomAccessibleInterval< UnsignedShortType > convertIntegerRAIToShort(
			RandomAccessibleInterval< ? > raiXYZ
	)
	{
		Object type = raiXYZ.getType();

		if ( type instanceof UnsignedShortType )
		{
			return (RandomAccessibleInterval <UnsignedShortType >) raiXYZ;
		}
		else if ( type instanceof UnsignedByteType )
		{
			return Converters.convert(
					raiXYZ,
					( i, o ) -> o.setInteger( ((UnsignedByteType) i).get() ),
					new UnsignedShortType( ) );
		}
		else
		{
			return Converters.convert(
					raiXYZ,
					( i, o ) ->
					{
						o.setInteger(((IntegerType)i).getInteger());
					},
					new UnsignedShortType( ) );
		}

	}
	
	@SuppressWarnings( "unchecked" )
	public static  <R extends RealType< R > > RandomAccessibleInterval< UnsignedShortType > convertRealRAIToShort(
			RandomAccessibleInterval< ? > raiXYZ,
			ValuePair< Double, Double > minMax)
	{
		return Converters.convert(
				( RandomAccessibleInterval< R > ) raiXYZ,
				new RealUnsignedShortConverter<>( minMax.getA(), minMax.getB() ),
				new UnsignedShortType() );
	}

	public static RandomAccessibleInterval< UnsignedShortType > convertAnnotationRAIToShort(
			RandomAccessibleInterval< ? > raiXYZ )
	{
		return Converters.convert(
				( RandomAccessibleInterval< AnnotationType< ? > > ) raiXYZ,
				( i, o ) ->
                {
					Annotation annotation = ( Annotation ) i.getAnnotation();
					if ( annotation != null ) // background pixel
					{
						final int label = annotation.label();
						o.setInteger( label );
						System.out.println( "" + label );
					}
                },
				new UnsignedShortType() );
	}
	
	//taken from LabKit
	//https://github.com/juglab/labkit-ui/blob/01a5c8058459a0d1a2eedc10f7212f64e021f893/src/main/java/sc/fiji/labkit/ui/bdv/BdvAutoContrast.java#L51
	private static ValuePair<Double, Double> getMinMax(final Source<?> src) 
	{
		int level = src.getNumMipmapLevels() - 1;
		RandomAccessibleInterval<?> source = src.getSource(0, level);
		if ( source.getType() instanceof RealType)
			return getMinMaxForRealType(Cast.unchecked(source));
		return new ValuePair<>(0.0, 255.0);
	}
	
	private static ValuePair<Double, Double> getMinMaxForRealType(
			RandomAccessibleInterval<? extends RealType<?>> source)
		{
			Cursor<? extends RealType<?>> cursor = Views.iterable(source).cursor();
			if (!cursor.hasNext()) return new ValuePair<>(0.0, 255.0);
			long stepSize = Intervals.numElements(source) / 10000 + 1;
			int randomLimit = (int) Math.min(Integer.MAX_VALUE, stepSize);
			Random random = new Random(42);
			double min = cursor.next().getRealDouble();
			double max = min;
			while (cursor.hasNext()) {
				double value = cursor.get().getRealDouble();
				cursor.jumpFwd(stepSize + random.nextInt(randomLimit));
				min = Math.min(min, value);
				max = Math.max(max, value);
			}
			return new ValuePair<>(min, max);
		}

}
