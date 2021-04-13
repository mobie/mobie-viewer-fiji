package de.embl.cba.mobie2.view;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.mobie.n5.source.LabelSource;
import de.embl.cba.mobie2.MoBIE2;
import de.embl.cba.mobie2.bdv.BdvLocationLogger;
import de.embl.cba.mobie2.bdv.SourcesAtMousePositionSupplier;
import de.embl.cba.mobie2.color.AdjustableOpacityColorConverter;
import de.embl.cba.mobie2.color.LabelConverter;
import de.embl.cba.mobie2.color.VolatileAdjustableOpacityColorConverter;
import de.embl.cba.mobie2.display.ImageDisplay;
import de.embl.cba.mobie2.display.SegmentationDisplay;
import de.embl.cba.mobie2.display.SourceDisplay;
import de.embl.cba.mobie2.segment.SegmentAdapter;
import de.embl.cba.mobie2.segment.BdvSegmentSelector;
import de.embl.cba.mobie2.source.ImageSource;
import de.embl.cba.mobie2.source.SegmentationSource;
import de.embl.cba.mobie2.transform.SourceTransformerSupplier;
import de.embl.cba.mobie2.ui.UserInterfaceHelper;
import de.embl.cba.tables.color.ColorUtils;
import de.embl.cba.tables.color.ColoringListener;
import de.embl.cba.tables.imagesegment.ImageSegment;
import de.embl.cba.tables.select.SelectionListener;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import mpicbg.spim.data.SpimData;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.bdv.MinimalBdvCreator;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformChanger;
import sc.fiji.bdvpg.bdv.projector.BlendingMode;
import sc.fiji.bdvpg.bdv.projector.Projector;
import sc.fiji.bdvpg.behaviour.SourceAndConverterContextMenuClickBehaviour;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;
import sc.fiji.bdvpg.sourceandconverter.display.ConverterChanger;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ImageViewer< S extends ImageSegment > implements ColoringListener, SelectionListener< S >
{
	private final MoBIE2 moBIE2;
	private final SourceAndConverterBdvDisplayService displayService;
	private final BdvHandle bdvHandle;
	private final boolean is2D;
	private final SourceDisplayManager< ?, ? > sourceDisplayManager;
	private Map< SelectionModel< TableRowImageSegment >, SegmentAdapter< TableRowImageSegment > > selectionModelToAdapter;
	private SourceAndConverterContextMenuClickBehaviour contextMenu;
	private final SourceAndConverterService sourceAndConverterService;
	private List< SourceDisplay > sourceDisplays;

	public ImageViewer( MoBIE2 moBIE2, boolean is2D, SourceDisplayManager sourceDisplayManager,  int timepoints )
	{
		this.moBIE2 = moBIE2;
		this.is2D = is2D;
		this.sourceDisplayManager = sourceDisplayManager;
		displayService = SourceAndConverterServices.getSourceAndConverterDisplayService();
		sourceAndConverterService = ( SourceAndConverterService ) SourceAndConverterServices.getSourceAndConverterService();

		// init Bdv
		bdvHandle = createBdv( timepoints );
		displayService.registerBdvHandle( bdvHandle );

		// init other stuff
		sourceDisplays = new ArrayList<>();
		selectionModelToAdapter = new ConcurrentHashMap<>();

		// register context menu actions
		installContextMenu();
	}

	private void installContextMenu( )
	{
		final SourceAndConverterService sourceAndConverterService;
		sourceAndConverterService = ( SourceAndConverterService ) SourceAndConverterServices.getSourceAndConverterService();
		//sourceAndConverterService.registerScijavaCommand( BdvLocationLogger.class );

		final Set< String > actionsKeys = sourceAndConverterService.getActionsKeys();
		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		final String[] actions = { "BDV - Screenshot", BdvLocationLogger.NAME, "Set Sources Projection Mode"  };

		contextMenu = new SourceAndConverterContextMenuClickBehaviour( bdvHandle, new SourcesAtMousePositionSupplier( bdvHandle, is2D ), actions );
		behaviours.behaviour( contextMenu, "Context menu", "button3", "shift P");
		behaviours.install( bdvHandle.getTriggerbindings(), "MoBIE" );
		final BdvSegmentSelector segmentBdvSelector = new BdvSegmentSelector( bdvHandle, is2D, () -> sourceDisplayManager.getSegmentationDisplays() );

		behaviours.behaviour(
				( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> segmentBdvSelector.run() ).start(),
				"Toggle selection", "ctrl button1" ) ;
	}

	private BdvHandle createBdv( int numTimepoints )
	{
		// create Bdv
		final MinimalBdvCreator bdvCreator = new MinimalBdvCreator( "MoBIE", is2D, Projector.MIXED_PROJECTOR, true, numTimepoints );
		final BdvHandle bdvHandle = bdvCreator.get();

		// configure size and location on screen
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		SwingUtilities.getWindowAncestor( bdvHandle.getViewerPanel() ).setSize( UserInterfaceHelper.getDefaultWindowWidth(), (int) ( screenSize.height * 0.7 ) );

		return bdvHandle;
	}

	public void show( ImageDisplay imageDisplay, List< SourceTransformerSupplier > sourceTransforms )
	{
		addSourceDisplay( imageDisplay );

		// open
		List< SourceAndConverter< ? > > sourceAndConverters = new ArrayList<>();
		for ( String sourceName : imageDisplay.getSources() )
		{
			final ImageSource source = moBIE2.getSource( sourceName );
			final SpimData spimData = BdvUtils.openSpimData( moBIE2.getImageLocation( source ) );
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

		sourceAndConverterService.getUI().hide();

		imageDisplay.sourceAndConverters = displayedSourceAndConverters;
	}

	private void addSourceDisplay( SourceDisplay imageDisplay )
	{
		imageDisplay.imageViewer = this;
		sourceDisplays.add( imageDisplay );
	}

	public void show( SegmentationDisplay display )
	{
		addSourceDisplay( display );

		display.selectionModel.listeners().add( this );
		display.coloringModel.listeners().add( this );

		final ArrayList< SourceAndConverter< ? > > sourceAndConverters = new ArrayList<>();

		for ( String sourceName : display.getSources() )
		{
			final SegmentationSource source = ( SegmentationSource ) moBIE2.getSource( sourceName );
			final SpimData spimData = BdvUtils.openSpimData( moBIE2.getImageLocation( source ) );
			final SourceAndConverter sourceAndConverter = SourceAndConverterHelper.createSourceAndConverters( spimData ).get( 0 );

			LabelConverter< S > labelConverter = new LabelConverter(
					display.segmentAdapter,
					sourceName,
					display.coloringModel );

			SourceAndConverter< ? > labelSourceAndConverter = asLabelSourceAndConverter( sourceAndConverter, labelConverter );

			sourceAndConverters.add( labelSourceAndConverter );

			displayService.show( bdvHandle, labelSourceAndConverter );
		}

		// TODO: maybe I should not keep track of this but on the fly filter
		//    all sources currently visible in BDV
		selectionModelToAdapter.put( display.selectionModel, display.segmentAdapter );

		sourceAndConverterService.getUI().hide();
		display.sourceAndConverters = sourceAndConverters;
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
		if ( selection.timePoint() != getBdvHandle().getViewerPanel().state().getCurrentTimepoint() )
		{
			getBdvHandle().getViewerPanel().state().setCurrentTimepoint( selection.timePoint() );
		}

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

	public void removeSourceDisplay( SourceDisplay sourceDisplay )
	{
		for ( SourceAndConverter< ? > sourceAndConverter : sourceDisplay.sourceAndConverters )
		{
			SourceAndConverterServices.getSourceAndConverterDisplayService().removeFromAllBdvs( sourceAndConverter );
		}
	}
}
