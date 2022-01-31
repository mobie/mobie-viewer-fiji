package org.embl.mobie.viewer.command;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Export Images to ImageJ1")
public class ImagePlusExportCommand< T extends NumericType< T > > implements BdvPlaygroundActionCommand
{
	@Parameter( label = "Source(s)" )
	SourceAndConverter< T >[] sourceAndConverterArray;

	@Override
	public void run()
	{
		final int level = 0; // TODO: make adjustable?

		final List< SourceAndConverter< T > > sourceAndConverters = Arrays.stream( this.sourceAndConverterArray ).collect( Collectors.toList() );

		final HashMap< SourceAndConverter< T >, AffineTransform3D > sacToTransform = fetchTransforms( sourceAndConverters );

		for ( SourceAndConverter< T > sourceAndConverter : sourceAndConverters )
		{
			final RandomAccessibleInterval< T > raiXYZT = getRAIXYZT( level, sourceAndConverter );
			// Permute to match dimension order
			final IntervalView< T > raiXYZTC = Views.addDimension( raiXYZT, 0, 0 );
			final IntervalView< T > raiXYCZT = Views.permute( raiXYZTC, 4, 2 );
			final ImagePlus imagePlus = ImageJFunctions.wrap( raiXYCZT, sourceAndConverter.getSpimSource().getName() );
			imagePlus.show();
		}
	}

	private RandomAccessibleInterval< T > getRAIXYZT( int level, SourceAndConverter< T > sourceAndConverter )
	{
		int maxTimepoint = 0;
		final Source< T > source = sourceAndConverter.getSpimSource();
		while ( source.isPresent( maxTimepoint ) )
		{
			maxTimepoint++;
			if ( maxTimepoint >= Integer.MAX_VALUE )
			{
				throw new RuntimeException("The source " + source.getName() + " appears to contain more than " + Integer.MAX_VALUE + " time points; maybe something is wrong?");
			}
		}

		return getRAIXYZT( level, maxTimepoint, source );
	}

	private RandomAccessibleInterval< T > getRAIXYZT( int level, int maxTimepoint, Source< T > source )
	{
		final ArrayList< RandomAccessibleInterval< T > > rais = new ArrayList<>();
		for ( int t = 0; t <= maxTimepoint; t++ )
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
