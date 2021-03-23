package de.embl.cba.mobie2.view;

import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.mobie2.*;
import de.embl.cba.mobie2.color.ColoringModelWrapper;
import de.embl.cba.mobie2.color.SegmentsConverter;
import de.embl.cba.tables.color.ColorUtils;
import de.embl.cba.tables.color.ColoringListener;
import de.embl.cba.tables.imagesegment.ImageSegment;
import de.embl.cba.tables.imagesegment.LabelFrameAndImage;
import de.embl.cba.tables.select.SelectionListener;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import mpicbg.spim.data.SpimData;
import sc.fiji.bdvpg.bdv.BdvCreator;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformChanger;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;
import sc.fiji.bdvpg.sourceandconverter.display.ConverterChanger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ImageViewer< T extends ImageSegment > implements ColoringListener, SelectionListener< T >
{
	private final MoBIE moBIE;
	private final SourceAndConverterBdvDisplayService displayService;
	private final BdvHandle bdvHandle;

	public ImageViewer( MoBIE moBIE )
	{
		this.moBIE = moBIE;
		displayService = SourceAndConverterServices.getSourceAndConverterDisplayService();
		BdvCreator creator = new BdvCreator( BdvOptions.options(), true, 1 );
		creator.run();
		bdvHandle = creator.get();
	}

	public List< SourceAndConverter< ? > > show( ImageDisplay imageDisplays )
	{
		final ArrayList< SourceAndConverter< ? > > sourceAndConverters = new ArrayList<>();

		for ( String sourceName : imageDisplays.sources )
		{
			final ImageSource source = ( ImageSource ) moBIE.getSource( sourceName );
			final SpimData spimData = BdvUtils.openSpimData( moBIE.getAbsoluteImageLocation( source ) );
			final SourceAndConverter sourceAndConverter = SourceAndConverterHelper.createSourceAndConverters( spimData ).get( 0 );

			new ColorChanger( sourceAndConverter, ColorUtils.getARGBType(  imageDisplays.color ) ).run();

			sourceAndConverters.add( sourceAndConverter );

			displayService.show( bdvHandle, sourceAndConverter );

			if ( imageDisplays.contrastLimits != null )
			{
				displayService.getConverterSetup( sourceAndConverter ).setDisplayRange( imageDisplays.contrastLimits[ 0 ], imageDisplays.contrastLimits[ 1 ] );
			}
			else
			{
				// TODO: auto adjust contrast? may be expensive...
			}
		}

		return sourceAndConverters;
	}

	public List< SourceAndConverter< ? > > show( SegmentationDisplay segmentationDisplay, ColoringModelWrapper< TableRowImageSegment > coloringModel, HashMap< LabelFrameAndImage, TableRowImageSegment > labelMap )
	{
		final ArrayList< SourceAndConverter< ? > > sourceAndConverters = new ArrayList<>();

		for ( String sourceName : segmentationDisplay.sources )
		{
			final SegmentationSource source = ( SegmentationSource ) moBIE.getSource( sourceName );
			final SpimData spimData = BdvUtils.openSpimData( moBIE.getAbsoluteImageLocation( source ) );
			final SourceAndConverter sourceAndConverter = SourceAndConverterHelper.createSourceAndConverters( spimData ).get( 0 );

			SegmentsConverter segmentsConverter = new SegmentsConverter(
					labelMap,
					sourceName,
					coloringModel );

			final SourceAndConverter segmentsSource = new ConverterChanger( sourceAndConverter, segmentsConverter, segmentsConverter ).get();

			sourceAndConverters.add( segmentsSource );

			displayService.show( bdvHandle, segmentsSource );

			// TODO: think about the alpha!
		}

		return sourceAndConverters;
	}

	@Override
	public synchronized void coloringChanged()
	{
		bdvHandle.getViewerPanel().requestRepaint();
	}

	@Override
	public synchronized void selectionChanged()
	{
		bdvHandle.getViewerPanel().requestRepaint();
	}

	@Override
	public synchronized void focusEvent( T selection )
	{
		final double[] position = new double[ 3 ];
		selection.localize( position );

		new ViewerTransformChanger(
				bdvHandle,
				BdvHandleHelper.getViewerTransformWithNewCenter( bdvHandle, position ),
				false,
				500 ).run();
	}
}
