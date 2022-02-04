package org.embl.mobie.viewer.command;

import bdv.tools.transformation.TransformedSource;
import bdv.util.Affine3DHelpers;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.embl.mobie.viewer.ThreadUtils;
import org.embl.mobie.viewer.playground.BdvPlaygroundUtils;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Show " + ImagePlusExportCommand.RAW + " Data" )
public class ImagePlusExportCommand< T extends NumericType< T > > implements BdvPlaygroundActionCommand
{
	public static final String RAW = "Raw"; // aka "Array" or "Voxel Grid", ... (not sure yet...)

	@Parameter( label = "Source(s)" )
	public SourceAndConverter[] sourceAndConverterArray;

	@Parameter( label = "Maximum number of pixels in X" )
	public int maxNumX = 2000;

	@Parameter( label = "Maximum number of pixels in Y" )
	public int maxNumY = 2000;

	@Override
	public void run()
	{
		final long maxNumPixelsXY = ( long ) maxNumX * ( long ) maxNumY;
		if ( maxNumPixelsXY > Integer.MAX_VALUE )
		{
			IJ.showMessage( "The maximum number of pixels that can be shown in one plane are " + Integer.MAX_VALUE + ".\n" +
					"With the current choice of maxNumX and maxNumY a higher value could be reached: " + maxNumPixelsXY + ".\n" +
					"Please reduce the number of pixels in x or y.");
			return;
		}

		final List< SourceAndConverter< T > > sourceAndConverters = getSacs();

		for ( SourceAndConverter< T > sourceAndConverter : sourceAndConverters )
		{
			exportAsImagePlus( sourceAndConverter, (int) maxNumPixelsXY );
		}
	}

	private void exportAsImagePlus( SourceAndConverter< T > sourceAndConverter, int maxNumPixelsXY )
	{
		final Source< T > source = sourceAndConverter.getSpimSource();
		final Source< T > rootSource = getRootSource( source );

		if ( rootSource == null )
		{
			IJ.log( source.getName() + ": Consists of multiple sources and export to ImagePlus is not yet supported.");
			return;
		}

		IJ.log(source.getName() + ": " + RAW + " data = " + rootSource.getName() );

		int exportLevel = getExportLevel( source, rootSource, maxNumPixelsXY );

		if ( exportLevel == -1 )
		{
			IJ.log(source.getName() + " is too big at all resolution levels and thus cannot be exported.");
			return;
		}

		long[] dimensions = source.getSource( 0, exportLevel ).dimensionsAsLongArray();

		IJ.log( source.getName() + ": Exporting at resolution level = " + exportLevel );
		IJ.log( source.getName() + ": [nx, yz, nz] = " + Arrays.toString( dimensions ) );

		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( 0, exportLevel, sourceTransform );

		final AffineTransform3D rootSourceTransform = new AffineTransform3D();
		rootSource.getSourceTransform( 0, exportLevel, rootSourceTransform );

		double[] sourceScale = new double[ 3 ];
		double[] rootSourceScale = new double[ 3 ];

		for ( int d = 0; d < 3; d++ )
		{
			sourceScale[ d ] = Affine3DHelpers.extractScale( sourceTransform, d );
			rootSourceScale[ d ] = Affine3DHelpers.extractScale( rootSourceTransform, d );
		}

		IJ.log( source.getName() + ": Scale = " + Arrays.toString( sourceScale ) );
		IJ.log( source.getName() + ": Transform = " + sourceTransform );
		IJ.log( source.getName() + ": " + RAW + " data scale = " + Arrays.toString( rootSourceScale ) );
		IJ.log( source.getName() + ": " + RAW + " data transform = " + rootSourceTransform );

		final ImagePlus imagePlus = getImagePlus( rootSource, exportLevel );
		imagePlus.getCalibration().setUnit( rootSource.getVoxelDimensions().unit() );
		imagePlus.getCalibration().pixelWidth = rootSourceScale[ 0 ];
		imagePlus.getCalibration().pixelHeight = rootSourceScale[ 1 ];
		imagePlus.getCalibration().pixelDepth = rootSourceScale[ 2 ];

		imagePlus.show();

		IJ.log(source.getName() + ": Export done!" );
	}

	private int getExportLevel( Source< T > source, Source< T > rootSource, int maxNumPixelsXY )
	{
		final int numMipmapLevels = rootSource.getNumMipmapLevels();

		for ( int level = 0; level < numMipmapLevels; level++ )
		{
			long[] dimensions = source.getSource( 0, level ).dimensionsAsLongArray();
			double fractionMaxNumPixelsXY = ( double ) dimensions[ 0 ] * ( double ) dimensions[ 1 ] / maxNumPixelsXY;
			if ( fractionMaxNumPixelsXY < 1 )
			{
				return level;
			}
		}
		return -1;
	}

	private Source< T > getRootSource( Source< T > source )
	{
		final Set< Source< ? > > rootSources = new HashSet<>();
		BdvPlaygroundUtils.fetchRootSources( source, rootSources );
		if ( rootSources.size() > 1 )
		{
			return null;
		}
		final Source< T > rootSource = ( Source< T > ) rootSources.iterator().next();
		return rootSource;
	}

	private ImagePlus getImagePlus( Source< T > source, int level )
	{
		final RandomAccessibleInterval< T > raiXYZT = getRAIXYZT( source, level );
		final IntervalView< T > raiXYZTC = Views.addDimension( raiXYZT, 0, 0 );
		final IntervalView< T > raiXYCZT = Views.permute( Views.permute( raiXYZTC, 4, 3 ), 3, 2);
		final ImagePlus imagePlus = ImageJFunctions.wrap( raiXYCZT, source.getName() );
		return imagePlus;
	}

	private List< SourceAndConverter< T > > getSacs()
	{
		final List< SourceAndConverter< T > > sourceAndConverters = new ArrayList<>();
		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverterArray )
		{
			sourceAndConverters.add( ( SourceAndConverter< T > ) sourceAndConverter );
		}
		return sourceAndConverters;
	}

	private RandomAccessibleInterval< T > getRAIXYZT( Source< T > source, int level )
	{
		int numTimepoints = 0;
		while ( source.isPresent( numTimepoints ) )
		{
			numTimepoints++;
			if ( numTimepoints >= Integer.MAX_VALUE )
			{
				throw new RuntimeException("The source " + source.getName() + " appears to contain more than " + Integer.MAX_VALUE + " time points; maybe something is wrong?");
			}
		}

		return getRAIXYZT( source, level, numTimepoints );
	}

	private RandomAccessibleInterval< T > getRAIXYZT( Source< T > source, int level, int numTimepoints )
	{
		final ArrayList< RandomAccessibleInterval< T > > rais = new ArrayList<>();
		for ( int t = 0; t < numTimepoints; t++ )
		{
			rais.add( source.getSource( t, level ) );
		}
		return Views.stack( rais );
	}

	private HashMap< SourceAndConverter< T >, AffineTransform3D > fetchTransforms( List< SourceAndConverter< T > > movingSacs )
	{
		final HashMap< SourceAndConverter< T >, AffineTransform3D > sacToTransform = new HashMap<>();
		for ( SourceAndConverter movingSac : movingSacs )
		{
			final AffineTransform3D fixedTransform = new AffineTransform3D();
			( ( TransformedSource ) movingSac.getSpimSource()).getFixedTransform( fixedTransform );
			sacToTransform.put( movingSac, fixedTransform );
		}
		return sacToTransform;
	}
}
