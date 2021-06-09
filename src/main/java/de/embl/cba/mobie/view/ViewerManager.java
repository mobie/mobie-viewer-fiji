package de.embl.cba.mobie.view;

import bdv.util.BdvHandle;
import de.embl.cba.mobie.Constants;
import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.Utils;
import de.embl.cba.mobie.bdv.ImageSliceView;
import de.embl.cba.mobie.bdv.SegmentationImageSliceView;
import de.embl.cba.mobie.bdv.SliceViewer;
import de.embl.cba.mobie.color.ColoringModelHelper;
import de.embl.cba.mobie.grid.GridOverlaySourceDisplay;
import de.embl.cba.mobie.plot.ScatterPlotViewer;
import de.embl.cba.mobie.segment.SegmentAdapter;
import de.embl.cba.mobie.source.SegmentationSource;
import de.embl.cba.mobie.display.ImageSourceDisplay;
import de.embl.cba.mobie.display.SegmentationSourceDisplay;
import de.embl.cba.mobie.display.SourceDisplay;
import de.embl.cba.mobie.table.TableDataFormat;
import de.embl.cba.mobie.table.TableViewer;
import de.embl.cba.mobie.transform.*;
import de.embl.cba.mobie.ui.UserInterface;
import de.embl.cba.mobie.ui.WindowArrangementHelper;
import de.embl.cba.mobie.view.additionalviews.AdditionalViewsLoader;
import de.embl.cba.mobie.view.saving.ViewsSaver;
import de.embl.cba.mobie.volume.SegmentsVolumeView;
import de.embl.cba.mobie.volume.UniverseManager;
import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.TableRows;
import de.embl.cba.tables.select.DefaultSelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;


import javax.activation.UnsupportedDataTypeException;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.embl.cba.mobie.Utils.createAnnotatedImageSegmentsFromTableFile;
import static de.embl.cba.mobie.ui.UserInterfaceHelper.setMoBIESwingLookAndFeel;
import static de.embl.cba.mobie.ui.UserInterfaceHelper.resetSystemSwingLookAndFeel;

public class ViewerManager
{
	private final MoBIE moBIE2;
	private final UserInterface userInterface;
	private final SliceViewer sliceViewer;
	private ArrayList< SourceDisplay > sourceDisplays;
	private final BdvHandle bdvHandle;
	private GridOverlaySourceDisplay gridOverlayDisplay;
	private final UniverseManager universeManager;
	private final AdditionalViewsLoader additionalViewsLoader;
	private final ViewsSaver viewsSaver;

	public ViewerManager(MoBIE moBIE2, UserInterface userInterface, boolean is2D, int timepoints )
	{
		this.moBIE2 = moBIE2;
		this.userInterface = userInterface;
		sourceDisplays = new ArrayList<>();
		sliceViewer = new SliceViewer( is2D, this, timepoints );
		universeManager = new UniverseManager();
		bdvHandle = sliceViewer.get();
		additionalViewsLoader = new AdditionalViewsLoader( moBIE2 );
		viewsSaver = new ViewsSaver( moBIE2 );
	}

	public static void initScatterPlotViewer( SegmentationSourceDisplay display )
	{
		display.scatterPlotViewer = new ScatterPlotViewer<>( display.segments, display.selectionModel, display.coloringModel, new String[]{ Constants.ANCHOR_X, Constants.ANCHOR_Y }, new double[]{1.0, 1.0}, 0.5 );
		display.selectionModel.listeners().add( display.scatterPlotViewer );
		display.coloringModel.listeners().add( display.scatterPlotViewer );
		display.sliceViewer.getBdvHandle().getViewerPanel().addTimePointListener( display.scatterPlotViewer );

		if ( display.showScatterPlot() )
			display.scatterPlotViewer.show();
	}

	public static void initTableViewer( SegmentationSourceDisplay display  )
	{
		display.tableViewer = new TableViewer<>( display.segments, display.selectionModel, display.coloringModel, display.getName() ).show();
		display.selectionModel.listeners().add( display.tableViewer );
		display.coloringModel.listeners().add( display.tableViewer );
	}

	public ArrayList< SourceDisplay > getSourceDisplays()
	{
		return sourceDisplays;
	}

	public SliceViewer getSliceViewer()
	{
		return sliceViewer;
	}

	public AdditionalViewsLoader getAdditionalViewsLoader() { return additionalViewsLoader; }

	public ViewsSaver getViewsSaver() { return viewsSaver; }

	public View getCurrentView( String uiSelectionGroup, boolean isExclusive, boolean includeViewerTransform ) {

		List< SourceDisplay > viewSourceDisplays = new ArrayList<>();
		List< SourceTransformer > viewSourceTransforms = new ArrayList<>();

		for ( SourceDisplay sourceDisplay : sourceDisplays ) {
			SourceDisplay currentDisplay = null;
			if ( sourceDisplay instanceof ImageSourceDisplay ) {
				currentDisplay = new ImageSourceDisplay( (ImageSourceDisplay) sourceDisplay );
			} else if ( sourceDisplay instanceof  SegmentationSourceDisplay ) {
				currentDisplay = new SegmentationSourceDisplay( (SegmentationSourceDisplay) sourceDisplay );
			}

			if ( currentDisplay != null ) {
				viewSourceDisplays.add( currentDisplay );
			}

			// TODO - would be good to pick up any manual transforms here too. This would allow e.g. manual placement
			// of differing sized sources into a grid
			if ( sourceDisplay.sourceTransformers != null ) {
				for ( SourceTransformer sourceTransformer: sourceDisplay.sourceTransformers ) {
					if ( !viewSourceTransforms.contains( sourceTransformer ) ) {
						viewSourceTransforms.add( sourceTransformer );
					}
				}
			}
		}

		if ( includeViewerTransform ) {
			AffineTransform3D normalisedViewTransform = Utils.createNormalisedViewerTransform(bdvHandle, Utils.getMousePosition(bdvHandle));
			BdvLocationSupplier viewerTransform = new BdvLocationSupplier(new BdvLocation(BdvLocationType.NormalisedViewerTransform, normalisedViewTransform.getRowPackedCopy()));
			return new View(uiSelectionGroup, viewSourceDisplays, viewSourceTransforms, viewerTransform, isExclusive);
		} else {
			return new View(uiSelectionGroup, viewSourceDisplays, viewSourceTransforms, isExclusive);
		}
	}

	public synchronized void show(View view )
	{
		if ( view.isExclusive() )
		{
			removeAllSourceDisplays();
		}

		setMoBIESwingLookAndFeel();

		// show the displays if there are any
		final List< SourceDisplay > sourceDisplays = view.getSourceDisplays();
		if ( sourceDisplays != null )
		{
			for ( SourceDisplay sourceDisplay : sourceDisplays )
			{
				// TODO: why are there transforms done here and below...
				sourceDisplay.sourceTransformers = view.getSourceTransforms();
				showSourceDisplay( sourceDisplay );
			}
		}

		// ...more source transforms here, feels wrong
		createAndShowGridView( SwingUtilities.getWindowAncestor( sliceViewer.get().getViewerPanel() ), view.getSourceTransforms() );

		resetSystemSwingLookAndFeel();

		// adjust the viewer transform
		if ( view.getViewerTransform() != null )
		{
			BdvLocationChanger.moveToLocation( bdvHandle, view.getViewerTransform().get() );
		}
		else
		{
			if ( view.isExclusive() || this.sourceDisplays.size() == 1 )
			{
				// focus on the image that was added last
				final SourceDisplay sourceDisplay = this.sourceDisplays.get( this.sourceDisplays.size() - 1 );
				new ViewerTransformAdjuster( bdvHandle, sourceDisplay.sourceAndConverters.get( 0 ) ).run();
			}
		}
	}

	private void showSourceDisplay( SourceDisplay sourceDisplay )
	{
		if ( sourceDisplays.contains( sourceDisplay ) ) return;

		sourceDisplay.sliceViewer = sliceViewer;

		if ( sourceDisplay instanceof ImageSourceDisplay )
		{
			showImageDisplay( ( ImageSourceDisplay ) sourceDisplay );
		}
		else if ( sourceDisplay instanceof SegmentationSourceDisplay )
		{
			final SegmentationSourceDisplay segmentationDisplay = ( SegmentationSourceDisplay ) sourceDisplay;
			showSegmentationDisplay( segmentationDisplay );
		}

		userInterface.addSourceDisplay( sourceDisplay );
		sourceDisplays.add( sourceDisplay );
	}

	private void createAndShowGridView( Window window, List< SourceTransformer > sourceTransformers )
	{
		int i = 0; // TODO: can there be more than one?

		if ( sourceTransformers != null )
		{
			for ( SourceTransformer sourceTransformer : sourceTransformers )
			{
				if ( sourceTransformer instanceof GridSourceTransformer )
				{
					final String tableDataFolder = ( ( GridSourceTransformer ) sourceTransformer ).getTableDataFolder( TableDataFormat.TabDelimitedFile );

					if ( tableDataFolder != null )
					{
						gridOverlayDisplay = new GridOverlaySourceDisplay( moBIE2, bdvHandle,  "grid-" + (i++), tableDataFolder, ( GridSourceTransformer ) sourceTransformer );

						userInterface.addGridView( gridOverlayDisplay );
						sourceDisplays.add( gridOverlayDisplay );

						SwingUtilities.invokeLater( () ->
						{
							WindowArrangementHelper.bottomAlignWindow( window, gridOverlayDisplay.getTableViewer().getWindow() );
						} );
					}
				}
			}
		}
	}

	private void removeAllSourceDisplays()
	{
		// create a copy of the currently shown displays...
		final ArrayList< SourceDisplay > currentSourceDisplays = new ArrayList<>( sourceDisplays ) ;

		// ...such that we can remove the displays without
		// modifying the list that we iterate over
		for ( SourceDisplay sourceDisplay : currentSourceDisplays )
		{
			// removes display from all viewers and
			// also from the list of currently shown sourceDisplays
			removeSourceDisplay( sourceDisplay );
		}
	}

	private void showImageDisplay( ImageSourceDisplay imageDisplay )
	{
		imageDisplay.imageSliceView = new ImageSliceView( imageDisplay, bdvHandle, ( List< String > name ) -> moBIE2.openSourceAndConverters( name ) );
	}

	// TODO: own class: SegmentationDisplayConfigurator
	private void showSegmentationDisplay( SegmentationSourceDisplay segmentationDisplay )
	{
		fetchSegmentsFromTables( segmentationDisplay );

		segmentationDisplay.segmentAdapter = new SegmentAdapter( segmentationDisplay.segments );
		ColoringModelHelper.configureMoBIEColoringModel( segmentationDisplay );
		segmentationDisplay.selectionModel = new DefaultSelectionModel<>();
		segmentationDisplay.coloringModel.setSelectionModel(  segmentationDisplay.selectionModel );

		// set selected segments
		if ( segmentationDisplay.getSelectedSegmentIds() != null )
		{
			final List< TableRowImageSegment > segments = segmentationDisplay.segmentAdapter.getSegments( segmentationDisplay.getSelectedSegmentIds() );
			segmentationDisplay.selectionModel.setSelected( segments, true );
		}

		initSliceViewer( segmentationDisplay );
		initTableViewer( segmentationDisplay );
		initScatterPlotViewer( segmentationDisplay );
		initVolumeViewer( segmentationDisplay );

		SwingUtilities.invokeLater( () ->
		{
			WindowArrangementHelper.bottomAlignWindow( segmentationDisplay.sliceViewer.getWindow(), segmentationDisplay.tableViewer.getWindow() );
		} );
	}

	private void fetchSegmentsFromTables( SegmentationSourceDisplay segmentationDisplay )
	{
		segmentationDisplay.segments = new ArrayList<>();

		// load default tables
		for ( String sourceName : segmentationDisplay.getSources() )
		{
			final SegmentationSource source = ( SegmentationSource ) moBIE2.getSource( sourceName );

			final String defaultTablePath = moBIE2.getDefaultTablePath( source );

			final List< TableRowImageSegment > segments = createAnnotatedImageSegmentsFromTableFile( defaultTablePath, sourceName );

			segmentationDisplay.segments.addAll( segments );
		}

		// check  validity
		for ( TableRowImageSegment segment : segmentationDisplay.segments )
		{
			if ( segment.labelId() == 0 )
			{
				throw new UnsupportedOperationException( "The table contains rows (image segments) with label index 0, which is not supported and will lead to errors. Please change the table accordingly." );
			}
		}

		// load additional tables
		// TODO: This will not work like this for the grid view with multiple sources...
		for ( String sourceName : segmentationDisplay.getSources() )
		{
			final SegmentationSource source = ( SegmentationSource ) moBIE2.getSource( sourceName );

			final List< String > tables = segmentationDisplay.getTables();
			if ( tables != null )
			{
				for ( String table : tables )
				{
					final String tablePath = moBIE2.getTablePath( source.tableData.get( TableDataFormat.TabDelimitedFile ).relativePath, table );
					IJ.log( "Opening table:\n" + tablePath );
					final Map< String, List< String > > newColumns =
							TableColumns.openAndOrderNewColumns(
									segmentationDisplay.segments,
									Constants.SEGMENT_LABEL_ID,
									tablePath );
					newColumns.remove( Constants.SEGMENT_LABEL_ID );
					for ( String columnName : newColumns.keySet() )
					{
						try
						{
							Object[] values = TableColumns.asTypedArray( newColumns.get( columnName ) );
							TableRows.addColumn( segmentationDisplay.segments, columnName, values );
						} catch ( UnsupportedDataTypeException e )
						{
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	private void initSliceViewer( SegmentationSourceDisplay segmentationDisplay )
	{
		final SegmentationImageSliceView segmentationImageSliceView = new SegmentationImageSliceView<>( segmentationDisplay, bdvHandle, ( List< String > names ) -> moBIE2.openSourceAndConverters( names ) );
		segmentationDisplay.segmentationImageSliceView = segmentationImageSliceView;
	}

	private void initVolumeViewer( SegmentationSourceDisplay display )
	{
		display.segmentsVolumeViewer = new SegmentsVolumeView<>( display.selectionModel, display.coloringModel, display.sourceAndConverters, universeManager );
		display.segmentsVolumeViewer.showSegments( display.showSelectedSegmentsIn3d() );
		display.coloringModel.listeners().add( display.segmentsVolumeViewer );
		display.selectionModel.listeners().add( display.segmentsVolumeViewer );
	}

	public synchronized void removeSourceDisplay( SourceDisplay sourceDisplay )
	{
		if ( sourceDisplay instanceof SegmentationSourceDisplay )
		{
			final SegmentationSourceDisplay segmentationDisplay = ( SegmentationSourceDisplay ) sourceDisplay;
			segmentationDisplay.segmentationImageSliceView.close();
			segmentationDisplay.tableViewer.close();
			segmentationDisplay.scatterPlotViewer.close();
			segmentationDisplay.segmentsVolumeViewer.close();
		}
		else if ( sourceDisplay instanceof ImageSourceDisplay )
		{
			final ImageSourceDisplay imageDisplay = ( ImageSourceDisplay ) sourceDisplay;
			imageDisplay.imageSliceView.close();
		}
		else if ( sourceDisplay instanceof GridOverlaySourceDisplay )
		{
			final GridOverlaySourceDisplay gridOverlayDisplay = ( GridOverlaySourceDisplay ) sourceDisplay;
			gridOverlayDisplay.close();
		}

		userInterface.removeDisplaySettingsPanel( sourceDisplay );
		sourceDisplays.remove( sourceDisplay );
	}

	public Collection< SegmentationSourceDisplay > getSegmentationDisplays()
	{
		final List< SegmentationSourceDisplay > segmentationDisplays = getSourceDisplays().stream().filter( s -> s instanceof SegmentationSourceDisplay ).map( s -> ( SegmentationSourceDisplay ) s ).collect( Collectors.toList() );

		return segmentationDisplays;
	}

	public void close()
	{
		removeAllSourceDisplays();
		sliceViewer.getBdvHandle().close();
		universeManager.close();
	}
}
