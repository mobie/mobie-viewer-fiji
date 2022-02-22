package org.embl.mobie.viewer.command;

import bdv.tools.boundingbox.TransformedRealBoxSelectionDialog;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.bdv.BdvBoundingBoxDialog;
import org.embl.mobie.viewer.transform.SourceAndConverterCropper;
import org.embl.mobie.viewer.view.View;
import org.embl.mobie.viewer.view.ViewFromSourceAndConverterCreator;
import org.scijava.Initializable;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + CropSourcesCommand.NAME )
public class CropSourcesCommand extends DynamicCommand implements BdvPlaygroundActionCommand, Initializable
{
	static{ LegacyInjector.preinit(); }

	public static final String NAME = "Crop Source(s)";

	@Parameter( label = "Bdv" )
	BdvHandle bdvHandle;

	@Parameter( label = "Source(s)" )
	public SourceAndConverter[] sourceAndConverterArray;

	@Parameter( label = "Cropped Sources Suffix" )
	public String suffix = "_crop";

	@Parameter( label = "Copped Source UI Selection Group" )
	public String uiSelectionGroup;

	private MoBIE moBIE;

	@Override
	public void initialize() {

		moBIE = MoBIE.getInstance( bdvHandle );

		final MutableModuleItem< String > item =
				getInfo().getMutableInput("uiSelectionGroup", String.class);

		final ArrayList< String > choices = new ArrayList<>( moBIE.getViewManager().getUserInterface().getGroupingsToViews().keySet() );

		item.setChoices( choices );
	}

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
			final RealInterval maskInterval = result.getInterval();
			final AffineTransform3D maskTransform = new AffineTransform3D();
			result.getTransform( maskTransform );

			for ( SourceAndConverter sourceAndConverter : sourceAndConverters )
			{
				final SourceAndConverter cropSource = cropSource( maskInterval, maskTransform, sourceAndConverter );

				addViewToUi( moBIE, cropSource );
			}
		}).start();
	}

	private void addViewToUi( MoBIE moBIE, SourceAndConverter cropSource )
	{
		final ViewFromSourceAndConverterCreator creator = new ViewFromSourceAndConverterCreator( cropSource );
		final View view = creator.createView( uiSelectionGroup, false );

		moBIE.getViews().put( cropSource.getSpimSource().getName(), view );
		moBIE.getUserInterface().addView( cropSource.getSpimSource().getName(), view );

		moBIE.getViewManager().show( view );
	}

	private SourceAndConverter cropSource( RealInterval maskInterval, AffineTransform3D maskTransform, SourceAndConverter sourceAndConverter )
	{
		final SourceAndConverterCropper creator = new SourceAndConverterCropper<>( sourceAndConverter, sourceAndConverter.getSpimSource().getName() + suffix, maskInterval, maskTransform );

		return creator.get();
	}
}
