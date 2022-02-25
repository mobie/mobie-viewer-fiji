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
import org.embl.mobie.viewer.transform.SourceAndConverterCropper;
import org.embl.mobie.viewer.transform.TransformHelpers;
import org.embl.mobie.viewer.view.View;
import org.embl.mobie.viewer.view.ViewFromSourceAndConverterCreator;
import org.embl.mobie.viewer.view.saving.ViewsSaver;
import org.scijava.Initializable;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + AffineTransformSourcesCommand.NAME )
public class AffineTransformSourcesCommand implements BdvPlaygroundActionCommand
{
	static{ LegacyInjector.preinit(); }

	public static final String NAME = "Affine Transform Source(s)";

	@Parameter( label = "Bdv" )
	BdvHandle bdvHandle;

	@Parameter( label = "Source(s)" )
	public SourceAndConverter[] sourceAndConverterArray;

	@Parameter( label = "Affine Row 1" )
	public String affineRow1 = "1.0, 0.0, 0.0, 0.0";

	@Parameter( label = "Affine Row 2" )
	public String affineRow2 = "0.0, 1.0, 0.0, 0.0";

	@Parameter( label = "Affine Row 3" )
	public String affineRow3 = "0.0, 0.0, 1.0, 0.0";

	private MoBIE moBIE;

	@Override
	public void run()
	{
		final List< SourceAndConverter > sourceAndConverters = Arrays.stream( sourceAndConverterArray ).collect( Collectors.toList() );
		if ( sourceAndConverters.size() == 0 ) return;

		String affineTransform = String.join(",", affineRow1, affineRow2, affineRow3 );
		final AffineTransform3D affineTransform3D = TransformHelpers.toAffineTransform3D( affineTransform );

		for ( SourceAndConverter sourceAndConverter : sourceAndConverters )
		{
			final TransformedSource< ? > transformedSource = ( TransformedSource< ? > ) sourceAndConverter.getSpimSource();
			final AffineTransform3D fixedTransform = new AffineTransform3D();
			transformedSource.getFixedTransform( fixedTransform );
			fixedTransform.preConcatenate( affineTransform3D );
			transformedSource.setFixedTransform( fixedTransform );
		}

		bdvHandle.getViewerPanel().requestRepaint();
	}

}
