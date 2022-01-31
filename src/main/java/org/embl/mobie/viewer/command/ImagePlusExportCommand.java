package org.embl.mobie.viewer.command;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.tables.Utils;
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
import org.embl.mobie.viewer.source.SegmentationSource;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Export Images as ImagePlus")
public class ImagePlusExportCommand< T extends NumericType< T > > implements BdvPlaygroundActionCommand
{
	@Parameter( label = "Source(s)" )
	SourceAndConverter[] sourceAndConverterArray;

	@Override
	public void run()
	{
		final List< SourceAndConverter< T > > sourceAndConverters = getSacs();

		final ExecutorService executorService = ThreadUtils.executorService;
		for ( SourceAndConverter< T > sourceAndConverter : sourceAndConverters )
		{
			executorService.submit( () -> {
				final Source< T > source = sourceAndConverter.getSpimSource();
				final Source< T > rootSource = getRootSource( source );
				IJ.log("Exporting: " + rootSource.getName() + "..." );
				final ImagePlus imagePlus = getImagePlus( rootSource, 0 );
				imagePlus.show();
				IJ.log("Export done: " + rootSource.getName() + "!" );
			});
		}
	}

	private Source< T > getRootSource( Source< T > source )
	{
		final Set< Source< ? > > rootSources = new HashSet<>();
		BdvPlaygroundUtils.fetchRootSources( source, rootSources );
		if ( rootSources.size() > 1 )
		{
			throw new RuntimeException( source.getName() + " consists of multiple sources and export to ImagePlus is not supported.");
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
