package org.embl.mobie.viewer.command;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.bdv.BdvBoundingBoxDialog;
import org.embl.mobie.viewer.playground.SourceChanger;
import org.embl.mobie.viewer.transform.CroppedSource;
import org.embl.mobie.viewer.transform.SourceCropper;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.sourceandconverter.importer.SourceAndConverterDuplicator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + CroppedViewCommand.NAME )
public class CroppedViewCommand implements BdvPlaygroundActionCommand
{
	static{ LegacyInjector.preinit(); }

	public static final String NAME = "Crop Source(s)";

	@Parameter( label = "Bdv" )
	BdvHandle bdvHandle;

	@Parameter( label = "Source(s)" )
	public SourceAndConverter[] sourceAndConverterArray;

	@Override
	public void run()
	{

		final List< SourceAndConverter > sourceAndConverters = Arrays.stream( sourceAndConverterArray ).collect( Collectors.toList() );
		if ( sourceAndConverters.size() == 0 ) return;

		new Thread( () -> {
			final BdvBoundingBoxDialog boxDialog = new BdvBoundingBoxDialog( bdvHandle, sourceAndConverters );
			boxDialog.showRealBoxAndWaitForResult();
			final RealInterval interval = boxDialog.getInterval();

			for ( SourceAndConverter sourceAndConverter : sourceAndConverters )
			{
//				final SourceAndConverter< ? > crop = SourceCropper.crop( sourceAndConverter, sourceAndConverter.getSpimSource().getName() + "-crop", interval, true );

				final CroppedSource croppedSource = new CroppedSource<>( sourceAndConverter.getSpimSource(), sourceAndConverter.getSpimSource().getName() + "-crop", interval, true );

				final CroppedSource volatileCroppedSource = new CroppedSource<>( sourceAndConverter.asVolatile().getSpimSource(), sourceAndConverter.getSpimSource().getName() + "-crop", interval, true );

				final SourceAndConverter croppedSourceAndConverter = new SourceAndConverter( croppedSource, SourceAndConverterHelper.cloneConverter( sourceAndConverter.getConverter(), sourceAndConverter ), new SourceAndConverter( volatileCroppedSource, SourceAndConverterHelper.cloneConverter( sourceAndConverter.asVolatile().getConverter(), sourceAndConverter.asVolatile() ) ) );

				BdvFunctions.show( croppedSourceAndConverter );
			}
		}).start();

	}

	private HashMap< SourceAndConverter< ? >, AffineTransform3D > fetchTransforms( List< SourceAndConverter< ? > > sourceAndConverters )
	{
		final HashMap< SourceAndConverter< ? >, AffineTransform3D > sacToTransform = new HashMap<>();
		for ( SourceAndConverter movingSac : sourceAndConverters )
		{
			final AffineTransform3D fixedTransform = new AffineTransform3D();
			( ( TransformedSource ) movingSac.getSpimSource()).getFixedTransform( fixedTransform );
			sacToTransform.put( movingSac, fixedTransform );
		}
		return sacToTransform;
	}
}
