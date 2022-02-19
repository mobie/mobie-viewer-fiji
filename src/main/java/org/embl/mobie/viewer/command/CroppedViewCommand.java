package org.embl.mobie.viewer.command;

import bdv.tools.boundingbox.TransformedRealBoxSelectionDialog;
import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.bdv.BdvBoundingBoxDialog;
import org.embl.mobie.viewer.transform.MaskedSource;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

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
			boxDialog.showDialog();
			final TransformedRealBoxSelectionDialog.Result result = boxDialog.getResult();
			if ( ! result.isValid() ) return;

			final boolean test = result.asMask().test( new RealPoint( new double[]{ 0, 0, 0 } ) );
			final boolean test2 = result.asMask().test( new RealPoint( new double[]{ 130, 130, 140 } ) );

			for ( SourceAndConverter sourceAndConverter : sourceAndConverters )
			{
//				final SourceAndConverter< ? > crop = SourceCropper.crop( sourceAndConverter, sourceAndConverter.getSpimSource().getName() + "-crop", interval, true );

				final MaskedSource maskedSource = new MaskedSource<>( sourceAndConverter.getSpimSource(), sourceAndConverter.getSpimSource().getName() + "-crop", result.asMask(), true );

				final MaskedSource volatileMaskedSource = new MaskedSource<>( sourceAndConverter.asVolatile().getSpimSource(), sourceAndConverter.getSpimSource().getName() + "-crop", result.asMask(), true );

				final SourceAndConverter croppedSourceAndConverter = new SourceAndConverter( maskedSource, SourceAndConverterHelper.cloneConverter( sourceAndConverter.getConverter(), sourceAndConverter ), new SourceAndConverter( volatileMaskedSource, SourceAndConverterHelper.cloneConverter( sourceAndConverter.asVolatile().getConverter(), sourceAndConverter.asVolatile() ) ) );

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
