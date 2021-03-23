package de.embl.cba.mobie2.view;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.mobie.n5.source.LabelSource;
import de.embl.cba.mobie2.*;
import de.embl.cba.mobie2.color.ColoringModelWrapper;
import de.embl.cba.mobie2.color.SegmentsConverter;
import de.embl.cba.tables.color.ColorUtils;
import de.embl.cba.tables.imagesegment.LabelFrameAndImage;
import de.embl.cba.tables.imagesegment.SegmentUtils;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import mpicbg.spim.data.SpimData;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;
import sc.fiji.bdvpg.sourceandconverter.display.ConverterChanger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static de.embl.cba.mobie.utils.Utils.createAnnotatedImageSegmentsFromTableFile;

public class ImageViewer
{
	private final MoBIE moBIE;
	private final SourceAndConverterBdvDisplayService displayService;

	public ImageViewer( MoBIE moBIE )
	{
		this.moBIE = moBIE;
		displayService = SourceAndConverterServices.getSourceAndConverterDisplayService();
	}

	public List< SourceAndConverter< ? > > show( ImageDisplays imageDisplays )
	{
		final ArrayList< SourceAndConverter< ? > > sourceAndConverters = new ArrayList<>();

		for ( String sourceName : imageDisplays.sources )
		{
			final ImageSource source = ( ImageSource ) moBIE.getSource( sourceName );
			final SpimData spimData = BdvUtils.openSpimData( moBIE.getAbsoluteImageLocation( source ) );
			final SourceAndConverter sourceAndConverter = SourceAndConverterHelper.createSourceAndConverters( spimData ).get( 0 );

			new ColorChanger( sourceAndConverter, ColorUtils.getARGBType(  imageDisplays.color ) ).run();

			sourceAndConverters.add( sourceAndConverter );

			displayService.show( sourceAndConverter );

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

	public List< SourceAndConverter< ? > > show( SegmentationDisplays segmentationDisplays, ColoringModelWrapper< TableRowImageSegment > coloringModel, HashMap< LabelFrameAndImage, TableRowImageSegment > labelMap )
	{
		final ArrayList< SourceAndConverter< ? > > sourceAndConverters = new ArrayList<>();

		for ( String sourceName : segmentationDisplays.sources )
		{
			final SegmentationSource source = ( SegmentationSource ) moBIE.getSource( sourceName );
			final SpimData spimData = BdvUtils.openSpimData( moBIE.getAbsoluteImageLocation( source ) );
			final SourceAndConverter sourceAndConverter = SourceAndConverterHelper.createSourceAndConverters( spimData ).get( 0 );

			SegmentsConverter segmentsConverter = new SegmentsConverter(
					labelMap,
					sourceName,
					coloringModel );

			final SourceAndConverter< ? > segmentsSource = replaceConverter( sourceAndConverter, segmentsConverter );

			sourceAndConverters.add( segmentsSource );

			displayService.show( segmentsSource );

			// TODO: think about the alpha!
		}

		return sourceAndConverters;
	}

	public static < R extends NumericType< R > & RealType< R > > SourceAndConverter< R > replaceConverter( SourceAndConverter< R > source, Converter< RealType, ARGBType > converter )
	{
		LabelSource< R > labelVolatileSource = new LabelSource( source.asVolatile().getSpimSource() );
		SourceAndConverter volatileSourceAndConverter = new SourceAndConverter( labelVolatileSource , converter );
		LabelSource< R > labelSource = new LabelSource( source.getSpimSource() );
		SourceAndConverter sourceAndConverter = new SourceAndConverter( labelSource, converter, volatileSourceAndConverter );
		return sourceAndConverter;
	}

}
