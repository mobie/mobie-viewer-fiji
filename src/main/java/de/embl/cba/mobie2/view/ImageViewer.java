package de.embl.cba.mobie2.view;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.mobie.n5.source.LabelSource;
import de.embl.cba.mobie2.MoBIE2;
import de.embl.cba.mobie2.color.AdjustableOpacityColorConverter;
import de.embl.cba.mobie2.color.LabelConverter;
import de.embl.cba.mobie2.color.VolatileAdjustableOpacityColorConverter;
import de.embl.cba.mobie2.color.VolatileRealARGBColorConverter;
import de.embl.cba.mobie2.display.ImageDisplay;
import de.embl.cba.mobie2.display.SegmentationDisplay;
import de.embl.cba.mobie2.segment.SegmentAdapter;
import de.embl.cba.mobie2.source.ImageSource;
import de.embl.cba.mobie2.source.SegmentationSource;
import de.embl.cba.mobie2.transform.SourceTransformerSupplier;
import de.embl.cba.tables.color.ColorUtils;
import de.embl.cba.tables.color.ColoringListener;
import de.embl.cba.tables.imagesegment.ImageSegment;
import de.embl.cba.tables.select.SelectionListener;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import mpicbg.spim.data.SpimData;
import net.imglib2.RealPoint;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformChanger;
import sc.fiji.bdvpg.bdv.projector.BlendingMode;
import sc.fiji.bdvpg.bdv.projector.Projector;
import sc.fiji.bdvpg.scijava.command.bdv.BdvWindowCreatorCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;
import sc.fiji.bdvpg.sourceandconverter.display.ConverterChanger;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ImageViewer< S extends ImageSegment > implements ColoringListener, SelectionListener< S >
{
	private final MoBIE2 moBIE2;
	private final SourceAndConverterBdvDisplayService displayService;
	private final BdvHandle bdvHandle;
	private final boolean is2D;
	private ArrayList< SourceAndConverter< ? > > labelSources;
	private Map< SelectionModel< TableRowImageSegment >, SegmentAdapter< TableRowImageSegment > > selectionModelToAdapter;

	public ImageViewer( MoBIE2 moBIE2, boolean is2D )
	{
		this.moBIE2 = moBIE2;
		this.is2D = is2D;
		displayService = SourceAndConverterServices.getSourceAndConverterDisplayService();

		// init Bdv
		bdvHandle = createBdv();
		displayService.registerBdvHandle( bdvHandle );
		installBehaviours( bdvHandle );

		labelSources = new ArrayList<>();
		selectionModelToAdapter = new HashMap<>();
	}

	private void installBehaviours( BdvHandle bdvHandle )
	{
		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdvHandle.getBdvHandle().getTriggerbindings(), "MoBIE" );
		addSelectionBehaviour( behaviours );
	}

	private void addSelectionBehaviour( Behaviours behaviours )
	{
		behaviours.behaviour(
				( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> toggleSelectionAtMousePosition() ).start(),
				"-toggle-select", "ctrl button1" ); ;
	}

	private synchronized void toggleSelectionAtMousePosition()
	{
		// TODO: Replace by function
		final RealPoint globalMouseCoordinates = BdvUtils.getGlobalMouseCoordinates( bdvHandle );
		List< SourceAndConverter< ? > > sources = bdvHandle.getViewerPanel().state().getSources();
		final int timepoint = bdvHandle.getViewerPanel().state().getCurrentTimepoint();

		Set< SourceAndConverter< ? > > sourcesAtMousePosition = sources.stream()
				.filter( source -> SourceAndConverterHelper.isPositionWithinSourceInterval( source, globalMouseCoordinates, timepoint, is2D ) )
				.collect( Collectors.toSet() );

		for ( SourceAndConverter< ? > sourceAndConverter : sourcesAtMousePosition )
		{
			if ( labelSources.contains( sourceAndConverter ) )
			{
				Source< ? > source = sourceAndConverter.getSpimSource();
				if ( source instanceof LabelSource )
					source = ( ( LabelSource ) source ).getWrappedSource();

				final Double labelIndex = BdvUtils.getPixelValue( source, globalMouseCoordinates, timepoint );

				if ( labelIndex == 0 ) return;

				// The image viewer can show several sources that
				// are associated with selection models. (In fact already
				// one selection model can be associated to several sources
				// that are shown in parallel, and which share the same
				// feature table).
				// We thus check in all models whether the
				// selected segment is a part of that model.
				for ( SelectionModel< TableRowImageSegment > selectionModel : selectionModelToAdapter.keySet() )
				{
					final TableRowImageSegment segment = selectionModelToAdapter.get( selectionModel ).getSegment( labelIndex, timepoint, source.getName() );

					if ( segment != null)
					{
						selectionModel.toggle( segment );
						if ( selectionModel.isSelected( segment ) )
						{
							selectionModel.focus( segment );
						}
					}
				}
			}
		}
	}

	private BdvHandle createBdv()
	{
		final BdvWindowCreatorCommand command = new BdvWindowCreatorCommand();
		command.is2D = is2D;
		command.interpolate = true;
		command.nTimepoints = 1; // TODO
		command.windowTitle = "MoBIE"; // TODO
		command.projector = Projector.MIXED_PROJECTOR;
		command.run();

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		SwingUtilities.getWindowAncestor( command.bdvh.getViewerPanel() ).setSize( screenSize.width / 3, (int) ( screenSize.height * 0.7 ) );

		return command.bdvh;
	}

	public List< SourceAndConverter< ? > > show( ImageDisplay imageDisplay, List< SourceTransformerSupplier > sourceTransforms )
	{
		// open
		List< SourceAndConverter< ? > > sourceAndConverters = new ArrayList<>();
		for ( String sourceName : imageDisplay.getSources() )
		{
			final ImageSource source = moBIE2.getSource( sourceName );
			final SpimData spimData = BdvUtils.openSpimData( moBIE2.getAbsoluteImageLocation( source ) );
			final SourceAndConverter sourceAndConverter = SourceAndConverterHelper.createSourceAndConverters( spimData ).get( 0 );
			sourceAndConverters.add( sourceAndConverter );
		}

		// transform
		List< SourceAndConverter< ? > > transformedSourceAndConverters = new ArrayList<>( sourceAndConverters );
		if ( sourceTransforms != null )
		{
			for ( SourceTransformerSupplier sourceTransform : sourceTransforms )
			{
				transformedSourceAndConverters = sourceTransform.get().transform( transformedSourceAndConverters );
			}
		}

		// show
		List< SourceAndConverter< ? > > displayedSourceAndConverters = new ArrayList<>();
		for ( SourceAndConverter< ? > sourceAndConverter : transformedSourceAndConverters )
		{
			// replace converter such that one can change the opacity
			// (this changes the hash-code of the sourceAndConverter)

			// TODO: understand this madness
			final Converter< RealType, ARGBType > converter = ( Converter< RealType, ARGBType > ) sourceAndConverter.getConverter();
			final Converter< ? extends Volatile< ? >, ARGBType > volatileConverter = sourceAndConverter.asVolatile().getConverter();
			sourceAndConverter = new ConverterChanger( sourceAndConverter, new AdjustableOpacityColorConverter(  converter ), new VolatileAdjustableOpacityColorConverter( volatileConverter ) ).get();

			// adapt color
			new ColorChanger( sourceAndConverter, ColorUtils.getARGBType(  imageDisplay.getColor() ) ).run();

			// set blending mode
			if ( imageDisplay.getBlendingMode() != null )
				SourceAndConverterServices.getSourceAndConverterService().setMetadata( sourceAndConverter, BlendingMode.BLENDING_MODE, imageDisplay.getBlendingMode());

			// show
			displayService.show( bdvHandle, sourceAndConverter );

			// adapt contrast limits
			final ConverterSetup converterSetup = displayService.getConverterSetup( sourceAndConverter );
			converterSetup.setDisplayRange( imageDisplay.getContrastLimits()[ 0 ], imageDisplay.getContrastLimits()[ 1 ] );

			displayedSourceAndConverters.add( sourceAndConverter );
		}

		return displayedSourceAndConverters;
	}

	public List< SourceAndConverter< ? > > show( SegmentationDisplay display )
	{
		final ArrayList< SourceAndConverter< ? > > sourceAndConverters = new ArrayList<>();

		for ( String sourceName : display.getSources() )
		{
			final SegmentationSource source = ( SegmentationSource ) moBIE2.getSource( sourceName );
			final SpimData spimData = BdvUtils.openSpimData( moBIE2.getAbsoluteImageLocation( source ) );
			final SourceAndConverter sourceAndConverter = SourceAndConverterHelper.createSourceAndConverters( spimData ).get( 0 );

			LabelConverter< S > labelConverter = new LabelConverter(
					display.segmentAdapter,
					sourceName,
					display.coloringModel );

			SourceAndConverter< ? > labelSourceAndConverter = asLabelSourceAndConverter( sourceAndConverter, labelConverter );

			sourceAndConverters.add( labelSourceAndConverter );

			displayService.show( bdvHandle, labelSourceAndConverter );
		}

		labelSources.addAll( sourceAndConverters );
		selectionModelToAdapter.put( display.selectionModel, display.segmentAdapter );

		return sourceAndConverters;
	}

	private SourceAndConverter asLabelSourceAndConverter( SourceAndConverter< ? > sourceAndConverter, LabelConverter labelConverter )
	{
		LabelSource volatileLabelSource = new LabelSource( sourceAndConverter.asVolatile().getSpimSource() );
		SourceAndConverter volatileSourceAndConverter = new SourceAndConverter( volatileLabelSource, labelConverter );
		LabelSource labelSource = new LabelSource( sourceAndConverter.getSpimSource() );
		return new SourceAndConverter( labelSource, labelConverter, volatileSourceAndConverter );
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
	public synchronized void focusEvent( S selection )
	{
		final double[] position = new double[ 3 ];
		selection.localize( position );

		new ViewerTransformChanger(
				bdvHandle,
				BdvHandleHelper.getViewerTransformWithNewCenter( bdvHandle, position ),
				false,
				500 ).run();
	}

	public BdvHandle getBdvHandle()
	{
		return bdvHandle;
	}

	public Window getWindow()
	{
		return SwingUtilities.getWindowAncestor( bdvHandle.getViewerPanel() );
	}

}
