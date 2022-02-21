package org.embl.mobie.viewer.command;

import bdv.tools.boundingbox.TransformedRealBoxSelectionDialog;
import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.bdv.BdvBoundingBoxDialog;
import org.embl.mobie.viewer.playground.SourceAffineTransformer;
import org.embl.mobie.viewer.transform.MaskedSource;
import org.embl.mobie.viewer.view.View;
import org.embl.mobie.viewer.view.ViewFromSourceAndConverterCreator;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + CropSourcesCommand.NAME )
public class CropSourcesCommand implements BdvPlaygroundActionCommand
{
	static{ LegacyInjector.preinit(); }

	public static final String NAME = "Crop Source(s)";

	@Parameter( label = "Bdv" )
	BdvHandle bdvHandle;

	@Parameter( label = "Source(s)" )
	public SourceAndConverter[] sourceAndConverterArray;

	@Parameter( label = "Suffix" )
	public String suffix = "_crop";

	@Override
	public void run()
	{
		final List< SourceAndConverter > sourceAndConverters = Arrays.stream( sourceAndConverterArray ).collect( Collectors.toList() );
		if ( sourceAndConverters.size() == 0 ) return;

		final MoBIE moBIE = MoBIE.getInstance( bdvHandle );

		new Thread( () -> {
			final BdvBoundingBoxDialog boxDialog = new BdvBoundingBoxDialog( bdvHandle, sourceAndConverters );
			boxDialog.showDialog();
			final TransformedRealBoxSelectionDialog.Result result = boxDialog.getResult();
			if ( ! result.isValid() ) return;
			final RealInterval maskInterval = result.getInterval();
			final AffineTransform3D maskTransform = new AffineTransform3D();
			result.getTransform( maskTransform );

			for ( SourceAndConverter sourceAndConverter : sourceAndConverters )
			{
				final SourceAndConverter cropSource = cropSource( maskInterval, maskTransform, sourceAndConverter );

				final ViewFromSourceAndConverterCreator creator = new ViewFromSourceAndConverterCreator( sourceAndConverter );

				addViewToUi( moBIE, cropSource, creator );
			}
		}).start();
	}

	private void addViewToUi( MoBIE moBIE, SourceAndConverter cropSource, ViewFromSourceAndConverterCreator creator )
	{
		final Map< String, Map< String, View > > groupingsToViews = moBIE.getUserInterface().getGroupingsToViews();
		final View view = creator.createView( groupingsToViews.keySet().iterator().next() );

		moBIE.getViews().put( cropSource.getSpimSource().getName(), view );
		moBIE.getUserInterface().addView( cropSource.getSpimSource().getName(), view );

		moBIE.getViewManager().show( view );

	}

	private SourceAndConverter cropSource( RealInterval maskInterval, AffineTransform3D maskTransform, SourceAndConverter sourceAndConverter )
	{
		final MaskedSource maskedSource = new MaskedSource<>( sourceAndConverter.getSpimSource(), sourceAndConverter.getSpimSource().getName() + suffix, maskInterval.minAsDoubleArray(), maskInterval.maxAsDoubleArray(), maskTransform );

		final MaskedSource volatileMaskedSource = new MaskedSource<>( sourceAndConverter.asVolatile().getSpimSource(), sourceAndConverter.getSpimSource().getName() + suffix, maskInterval.minAsDoubleArray(), maskInterval.maxAsDoubleArray(), maskTransform );

		final SourceAndConverter maskedSourceAndConverter = new SourceAndConverter( maskedSource, SourceAndConverterHelper.cloneConverter( sourceAndConverter.getConverter(), sourceAndConverter ), new SourceAndConverter( volatileMaskedSource, SourceAndConverterHelper.cloneConverter( sourceAndConverter.asVolatile().getConverter(), sourceAndConverter.asVolatile() ) ) );

		// Wrap into TransformedSource for, e.g., manual transformation
		final SourceAndConverter sourceOut = new SourceAffineTransformer( maskedSourceAndConverter, new AffineTransform3D() ).getSourceOut();
		return sourceOut;
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
