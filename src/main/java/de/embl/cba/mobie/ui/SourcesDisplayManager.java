package de.embl.cba.mobie.ui;

import bdv.tools.transformation.TransformedSource;
import bdv.util.*;
import bdv.viewer.Source;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.bdv.utils.sources.ARGBConvertedRealSource;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.mobie.Constants;
import de.embl.cba.mobie.image.SourceAndMetadataChangedListener;
import de.embl.cba.mobie.image.SourceGroupLabelSourceMetadata;
import de.embl.cba.mobie.image.SourceGroupLabelSourceCreator;
import de.embl.cba.mobie.image.SourcesModel;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.FileUtils;
import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.color.ColorUtils;
import de.embl.cba.tables.color.LazyLabelsARGBConverter;
import de.embl.cba.tables.ij3d.UniverseUtils;
import de.embl.cba.tables.image.DefaultImageSourcesModel;
import de.embl.cba.tables.image.SourceAndMetadata;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import de.embl.cba.tables.view.Segments3dView;
import de.embl.cba.tables.view.SegmentsBdvView;
import de.embl.cba.tables.view.TableRowsTableView;
import de.embl.cba.tables.view.combined.SegmentsTableBdvAnd3dViews;
import ij3d.ContentConstants;
import ij3d.Image3DUniverse;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import org.jetbrains.annotations.NotNull;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.vecmath.Color3f;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.util.List;
import java.util.*;

import static de.embl.cba.mobie.utils.Utils.*;

public class SourcesDisplayManager extends JPanel
{
    private final Map< String, SegmentsTableBdvAnd3dViews > sourceNameToLabelViews;
    private Map< String, SourceAndMetadata< ? > > sourceNameToSourceAndCurrentMetadata;
    private BdvHandle bdv;
    private final SourcesModel sourcesModel;
    private final String projectName;
    private Image3DUniverse universe;
    private int meshSmoothingIterations = 5; // TODO: Why is this here?

    private List< SourceAndMetadataChangedListener > listeners = new ArrayList<>(  );

    public SourcesDisplayManager( SourcesModel sourcesModel, String projectName )
    {
        this.sourcesModel = sourcesModel;
        this.projectName = projectName;
        sourceNameToLabelViews = new LinkedHashMap<>();
        sourceNameToSourceAndCurrentMetadata = new LinkedHashMap<>();
    }

    public static void updateSource3dView( SourceAndMetadata< ? > sam, SourcesDisplayManager sourcesDisplayManager, boolean forceRepaint )
    {
        if ( sam.metadata().type.equals( Metadata.Type.Segmentation ) ) return;

        if ( sam.metadata().showImageIn3d )
        {
            sourcesDisplayManager.showSourceInVolumeViewer( sam, forceRepaint );
        }
        else
        {
            if ( sam.metadata().content != null )
                sam.metadata().content.setVisible( false );
        }
    }

    public static void updateSegments3dView( SourceAndMetadata< ? > sam, SourcesDisplayManager sourcesDisplayManager )
    {
        if ( sam.metadata().views != null )
        {
            final Segments3dView< TableRowImageSegment > segments3dView = sam.metadata().views.getSegments3dView();
            segments3dView.setVoxelSpacing( sam.metadata().resolution3dView );
            segments3dView.showSelectedSegments( sam.metadata().showSelectedSegmentsIn3d, false );

            if ( sam.metadata().showSelectedSegmentsIn3d ) sourcesDisplayManager.setUniverse( segments3dView.getUniverse() );
        }
    }

    public void setSourceColor( SourceAndMetadata< ? > sam, Color color )
    {
        sam.metadata().bdvStackSource.setColor( ColorUtils.getARGBType( color ) );

        if ( sam.metadata().content != null )
            sam.metadata().content.setColor( new Color3f( color ));

        if ( sourceNameToLabelViews.containsKey( sam.metadata().displayName ) )
        {
            final SegmentsBdvView< TableRowImageSegment > segmentsBdvView
                    = sourceNameToLabelViews.get( sam.metadata().displayName ).getSegmentsBdvView();

            segmentsBdvView.setLabelSourceSingleColor( ColorUtils.getARGBType( color ) );
        }
    }

    public void setSourceColor( String sourceName, Color color )
    {
        if ( ! sourceNameToSourceAndCurrentMetadata.containsKey( sourceName ) )
        {
            System.err.println( "Source not displayed: " + sourceName );
            return;
        }

        final SourceAndMetadata< ? > sam = sourceNameToSourceAndCurrentMetadata.get(sourceName);

        setSourceColor( sam, color );
    }

    public Image3DUniverse getUniverse()
    {
        return universe;
    }

    public int getMeshSmoothingIterations()
    {
        return meshSmoothingIterations;
    }

    public void setMeshSmoothingIterations( int meshSmoothingIterations )
    {
        this.meshSmoothingIterations = meshSmoothingIterations;

        for ( SegmentsTableBdvAnd3dViews views : sourceNameToLabelViews.values() )
            views.getSegments3dView().setMeshSmoothingIterations( meshSmoothingIterations );
    }

    public void setVoxelSpacing3DView( String sourceName, double voxelSpacing )
    {
        SourceAndMetadata< ? > sam = sourceNameToSourceAndCurrentMetadata.get( sourceName );

        if ( sam.metadata().resolution3dView != voxelSpacing )
        {
            sam.metadata().resolution3dView = voxelSpacing;

            if ( sam.metadata().content != null )
                updateSource3dView( sam, this, true );

            if ( sourceNameToLabelViews.containsKey( sourceName ) )
            {
                Segments3dView< TableRowImageSegment > segments3dView = sourceNameToLabelViews.get( sourceName ).getSegments3dView();
                segments3dView.setVoxelSpacing( voxelSpacing );
                segments3dView.showSelectedSegments( sam.metadata().showSelectedSegmentsIn3d, true );
            }
        }
    }

    public void updateCurrentMetadata( String sourceName )
    {
        final Metadata metadata = sourceNameToSourceAndCurrentMetadata.get( sourceName ).metadata();
        final Source< ? > source = sourceNameToSourceAndCurrentMetadata.get( sourceName ).source();
        final BdvStackSource< ? > bdvStackSource = metadata.bdvStackSource;

        metadata.color = bdvStackSource.getConverterSetups().get( 0 ).getColor().toString();
        metadata.addedTransform = getAddedSourceTransform( bdvStackSource, source ).getRowPackedCopy();

        if (sourceNameToLabelViews.containsKey(sourceName)) {
            TableRowsTableView<TableRowImageSegment> sourceTableRowsTableView = metadata.views.getTableRowsTableView();

            if (!metadata.views.getSegmentsBdvView().isLabelMaskShownAsBinaryMask()) {
                metadata.color = sourceTableRowsTableView.getColoringLUTName();
                metadata.colorByColumn = sourceTableRowsTableView.getColoringColumnName();
                metadata.valueLimits = sourceTableRowsTableView.getColorByColumnValueLimits();
            }

            ArrayList<TableRowImageSegment> selectedSegments = sourceTableRowsTableView.getSelectedLabelIds();
            if (selectedSegments != null) {
                ArrayList<Double> selectedLabelIds = new ArrayList<>();
                for (TableRowImageSegment segment : selectedSegments) {
                    selectedLabelIds.add(segment.labelId());
                }
                metadata.selectedSegmentIds = selectedLabelIds;
            }

            ArrayList<String> additionalTables = sourceTableRowsTableView.getAdditionalTables();
            if (additionalTables != null & additionalTables.size() > 0 ) {
                metadata.additionalSegmentTableNames = new ArrayList<>();
                // ensure tables are unique
                for (String tableName : sourceTableRowsTableView.getAdditionalTables()) {
                    if (!metadata.additionalSegmentTableNames.contains(tableName)) {
                        metadata.additionalSegmentTableNames.add(tableName);
                    }
                }
            }

            metadata.showSelectedSegmentsIn3d = metadata.views.getSegments3dView().showSelectedSegments();
        }

        if (metadata.content != null) {
            if (metadata.content.isVisible()) {
                metadata.showImageIn3d = true;
            } else {
                metadata.showImageIn3d = false;
            }
        } else {
            metadata.showImageIn3d = false;
        }

        double[] currentContrastLimits = new double[2];
        currentContrastLimits[0] = bdvStackSource.getConverterSetups().get(0).getDisplayRangeMin();
        currentContrastLimits[1] = bdvStackSource.getConverterSetups().get(0).getDisplayRangeMax();
        metadata.contrastLimits = currentContrastLimits;
    }

    @NotNull
    protected AffineTransform3D getAddedSourceTransform( BdvStackSource< ? > bdvStackSource, Source< ? > source )
    {
        final int t = 0; // TODO: Once we have data with multiple time points we may have to rethink this...
        final AffineTransform3D initialTransform = new AffineTransform3D();
        source.getSourceTransform( t, 0, initialTransform );
        final AffineTransform3D currentTransform = new AffineTransform3D();
        bdvStackSource.getSources().get( 0 ).getSpimSource().getSourceTransform( t, 0, currentTransform );
        return currentTransform.copy().preConcatenate( initialTransform.inverse() );
    }

    public void showSourceInVolumeViewer( SourceAndMetadata< ? > sam, boolean forceRepaint )
    {
    	if ( sam.metadata().showImageIn3d )
        {
            initAndShowUniverseIfNecessary();

            if ( sam.metadata().content == null || forceRepaint )
            {
                DisplaySettings3DViewer settings = getDisplaySettings3DViewer( sam );

                if ( forceRepaint )
                {
                    if ( universe.getContents().contains( sam.metadata().content ) )
                    {
                        universe.removeContent( sam.metadata().content.getName() );
                    }
                }

                if ( sam.metadata().resolution3dView == 0 )
                {
                    // auto-adjust voxel spacing using maxNumVoxels
                    sam.metadata().content = UniverseUtils.addSourceToUniverse(
                            universe,
                            sam.source(),
                            300 * 300 * 300, // TODO: make adaptable
                            settings.displayMode,
                            settings.color,
                            settings.transparency,
                            0,
                            settings.max );
                }
                else
                {
                    sam.metadata().content = UniverseUtils.addSourceToUniverse(
                            universe,
                            sam.source(),
                            sam.metadata().resolution3dView,
                            settings.displayMode,
                            settings.color,
                            settings.transparency,
                            0,
                            settings.max );
                }
            }
            else
            {
                if ( ! universe.getContents().contains( sam.metadata().content ) )
                    universe.addContent( sam.metadata().content );

                sam.metadata().content.setVisible( true );
            }
        }
    	else
        {
            return;
        }
    }

    public void initAndShowUniverseIfNecessary()
    {
        if ( universe == null ) init3DUniverse();

        UniverseUtils.showUniverseWindow( universe, bdv.getViewerPanel() );
    }

    private void init3DUniverse()
	{
		universe = new Image3DUniverse();
	}

    public void setUniverse( Image3DUniverse universe )
    {
        this.universe = universe;
    }

    public void close()
    {

    }

    class DisplaySettings3DViewer
    {
        int displayMode;
        float transparency;
        int max;
        ARGBType color = new ARGBType( 0xffffffff );
    }

    private DisplaySettings3DViewer getDisplaySettings3DViewer( SourceAndMetadata< ? > sam )
    {
        final DisplaySettings3DViewer settings = new DisplaySettings3DViewer();
        if ( sam.metadata().displayName.contains( Constants.PROSPR ) // TODO: make more general
                || sam.metadata().displayName.contains( Constants.SEGMENTED )  )
        {
            settings.displayMode = ContentConstants.SURFACE;
            settings.max = 1;
            settings.transparency = 0.3F;
        }
        else if ( sam.metadata().displayName.contains( Constants.EM_FILE_ID ) )
        {
            settings.displayMode = ContentConstants.ORTHO;
            settings.max = 255;
            settings.transparency = 0.0F;
        }
        else
        {
            // do nothing
        }

        if ( sam.metadata().content != null )
        {
            final ARGBType argbType = ColorUtils.getARGBType( sam.metadata().content.getColor().get() );
            if ( argbType != null ) settings.color = argbType;
        }
        else
        {
            final ARGBType argbType = ColorUtils.getARGBType( sam.metadata().color );
            if ( argbType != null ) settings.color = argbType;
        }

        return settings;
    }

    public void show( String sourceName )
    {
        final SourceAndMetadata< ? > samDefault = getSourceAndDefaultMetadata( sourceName );

        // make a copy here so that changes to the current metadata, don't affect the default
        // this means any changes to current metadata won't persist when sources are removed and added again
        final SourceAndMetadata< ? > samCurrent = new SourceAndMetadata( samDefault.source(), samDefault.metadata().copy() );

        show( samCurrent );
    }

    public SourceAndMetadata< ? > getSourceAndDefaultMetadata( String sourceName )
    {
        return sourcesModel.sources().get( sourceName );
    }

    // necessary as loading from bookmark creates a new sourceAndMetadata that is separate from the default
    public SourceAndMetadata< ? > getSourceAndCurrentMetadata( String sourceName )
    {
        return sourceNameToSourceAndCurrentMetadata.get( sourceName );
    }

    public ArrayList< String > getSourceNames()
    {
        return new ArrayList<>( sourcesModel.sources().keySet() );
    }

    public Set< String > getVisibleSourceNames()
    {
        return Collections.unmodifiableSet( new HashSet<>( sourceNameToSourceAndCurrentMetadata.keySet() ) );
    }

    public void show( SourceAndMetadata< ? > sam )
    {
        final String sourceName = sam.metadata().displayName;

        if ( sam.metadata().bdvStackSource != null  )
        {
            Logger.log( "Source is already shown: " + sourceName + "" );
            return;
        }

        Logger.log( "Adding source: " + sourceName + "..." );
        sourceNameToSourceAndCurrentMetadata.put( sourceName, sam );

        Prefs.showScaleBar( true );
        Prefs.showMultibox( false );

        final Metadata metadata = sam.metadata();

        if ( metadata.type.equals( Metadata.Type.Segmentation ) )
        {
            if ( metadata.segmentsTablePath != null )
            {
                showAnnotatedLabelsSource( sam );
            }
            else
            {
                showLabelsSource( sam );
            }
        }
        else if ( metadata.type.equals( Metadata.Type.Image ) )
        {
            showIntensitySource( sam );
        }
        else if ( metadata.type.equals( Metadata.Type.Mask ) )
        {
            showIntensitySource( sam );
        }

        adjustSourceTransform( sam );
        for ( SourceAndMetadataChangedListener listener : listeners )
        {
            listener.addedToBDV( sam );
        }
    }

    private void adjustSourceTransform( SourceAndMetadata< ? > sam )
    {
        if ( sam.metadata().addedTransform != null )
        {
            final TransformedSource< ? > source = ( TransformedSource< ? > ) sam.metadata().bdvStackSource.getSources().get( 0 ).getSpimSource();
            final AffineTransform3D transform = new AffineTransform3D();
            transform.set( sam.metadata().addedTransform );
            source.setFixedTransform( transform );
        }
    }

    private void showIntensitySource( SourceAndMetadata< ? > sam )
    {
        final Metadata metadata = sam.metadata();

        final BdvStackSource< ? > bdvStackSource = addSourceToBDV( sam );
        bdvStackSource.setActive( true );

        setDisplayRange( bdvStackSource, metadata );
        setColor( bdvStackSource, metadata );

        bdv = bdvStackSource.getBdvHandle();

        metadata.bdvStackSource = bdvStackSource;
    }

    @NotNull
    private BdvStackSource addSourceToBDV( SourceAndMetadata< ? > sam )
    {
        // TODO: Why do we have numRenderingThreads = 1?
        BdvOptions options = BdvOptions.options().addTo( bdv ).frameTitle( projectName ).numRenderingThreads( 1 );

        return BdvFunctions.show(
                sam.source(),
                1, // TODO: Why is this needed? How could we determine it?
                options );
    }

    private void setColor( BdvStackSource bdvStackSource, Metadata metadata )
    {
        ARGBType argbType;
        if ( metadata.color.equals( Constants.RANDOM_FROM_GLASBEY ) )
        {
            final GlasbeyARGBLut glasbeyARGBLut = new GlasbeyARGBLut();
            final int argb = glasbeyARGBLut.getARGB( createRandom( metadata.imageId ) );
            argbType = new ARGBType( argb );
        }
        else
        {
            argbType = ColorUtils.getARGBType( metadata.color );
        }

        if ( argbType != null ) bdvStackSource.setColor( argbType );
    }

    private void setDisplayRange( BdvStackSource bdvStackSource, Metadata metadata )
    {
        if ( metadata.contrastLimits != null )
        {
            bdvStackSource.setDisplayRange( metadata.contrastLimits[ 0 ], metadata.contrastLimits[ 1 ] );
        }
    }

    // TODO: probably this should be an own class, part of the table-utils views framework
    private void showLabelsSource( SourceAndMetadata< ? > sam )
    {
        final LazyLabelsARGBConverter lazyLabelsARGBConverter = new LazyLabelsARGBConverter();
        final ARGBConvertedRealSource source =
                new ARGBConvertedRealSource( sam.source(),
                        lazyLabelsARGBConverter );

        sam.metadata().bdvStackSource = addSourceToBDV( sam );

        setDisplayRange( sam.metadata().bdvStackSource, sam.metadata() );

        final Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
        behaviours.install( bdv.getBdvHandle().getTriggerbindings(), sam.metadata().displayName + "-bdv-select-handler" );

        behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
                new Thread( () ->
                {
                    lazyLabelsARGBConverter.getColoringModel().incRandomSeed();
                    BdvUtils.repaint( bdv );
                } ).start(),
                sam.metadata().displayName + "-change-color-random-seed",
                "ctrl L" );
    }

    private void showAnnotatedLabelsSource( SourceAndMetadata< ? > sam )
    {
        List< TableRowImageSegment > segments;
        if ( sam.metadata().misc.containsKey( SourceGroupLabelSourceCreator.SOURCE_GROUP_LABEL_IMAGE_METADATA ) )
        {
            segments = createGroupedSourcesSegmentsFromTableFile(
                    sam.metadata().segmentsTablePath,
                    sam.metadata().groupId,
                    ( SourceGroupLabelSourceMetadata ) sam.metadata().misc.get( SourceGroupLabelSourceCreator.SOURCE_GROUP_LABEL_IMAGE_METADATA )
                    );
        }
        else
        {
            segments = createAnnotatedImageSegmentsFromTableFile(
                    sam.metadata().segmentsTablePath,
                    sam.metadata().imageId );
        }

		setUniverse();

        final SegmentsTableBdvAnd3dViews views
                = new SegmentsTableBdvAnd3dViews(
                    segments,
                    createLabelsSourceModel( sam ),
                    sam.metadata().imageId,
                    bdv,
                    universe );

        sam.metadata().views = views;
        configureSegments3dView( views, sam );
        configureTableView( views, sam );

        bdv = views.getSegmentsBdvView().getBdv();

        sam.metadata().bdvStackSource = views
                .getSegmentsBdvView()
                .getCurrentSources().get( 0 )
                .metadata().bdvStackSource;

        setDisplayRange( sam.metadata().bdvStackSource, sam.metadata() );

        sourceNameToLabelViews.put( sam.metadata().displayName, views );
    }

	private void setUniverse()
	{
		for ( SegmentsTableBdvAnd3dViews views : sourceNameToLabelViews.values() )
		{
			if ( views.getSegments3dView().getUniverse() != null )
			{
				universe = views.getSegments3dView().getUniverse();
				continue;
			}
		}
	}

	private void configureTableView( SegmentsTableBdvAnd3dViews views, SourceAndMetadata< ? > sam )
    {
        final TableRowsTableView< TableRowImageSegment > tableRowsTableView = views.getTableRowsTableView();

        final String tablesLocation = FileAndUrlUtils.getParentLocation( sam.metadata().segmentsTablePath );
        tableRowsTableView.setTablesDirectory( tablesLocation );
        tableRowsTableView.setMergeByColumnName( Constants.SEGMENT_LABEL_ID );
        //tableRowsTableView.setSelectionMode( TableRowsTableView.SelectionMode.FocusOnly );

        mergeDefaultTableWithAdditionalTables( sam, tableRowsTableView, tablesLocation );

        // select segments
        if ( sam.metadata().selectedSegmentIds != null && sam.metadata().selectedSegmentIds.size() > 0 )
        {
            // TODO: in table-utils: the selection should not be part of any specific view
            views.getSegmentsBdvView().select( sam.metadata().selectedSegmentIds );
        }

        // apply colorByColumn
        if ( sam.metadata().colorByColumn != null && sam.metadata().color != null )
        {
            if ( sam.metadata().valueLimits != null ) {
                views.getTableRowsTableView().colorByColumn(
                        sam.metadata().colorByColumn,
                        sam.metadata().color,
                        sam.metadata().valueLimits[0],
                        sam.metadata().valueLimits[1]);
            } else {
                views.getTableRowsTableView().colorByColumn(
                        sam.metadata().colorByColumn,
                        sam.metadata().color,
                        null,
                        null);
            }
        }
    }

    private void mergeDefaultTableWithAdditionalTables( SourceAndMetadata< ? > sam, TableRowsTableView< TableRowImageSegment > tableRowsTableView, String tablesLocation )
    {
        if ( sam.metadata().additionalSegmentTableNames == null ) return;

        for ( String tableName : sam.metadata().additionalSegmentTableNames )
        {
            String newTablePath = FileAndUrlUtils.combinePath( tablesLocation, tableName + ".csv" );
            tableRowsTableView.addAdditionalTable(newTablePath);

            if ( newTablePath.startsWith( "http" ) )
                newTablePath = FileUtils.resolveTableURL( URI.create( newTablePath ) );

            final Map< String, List< String > > newColumns =
                    TableColumns.openAndOrderNewColumns(
                        tableRowsTableView.getTable(),
                        Constants.SEGMENT_LABEL_ID,
                        newTablePath );

            newColumns.remove( Constants.SEGMENT_LABEL_ID );
            tableRowsTableView.addColumns( newColumns );
        }
    }

    private void configureSegments3dView( SegmentsTableBdvAnd3dViews views, SourceAndMetadata< ? > sam )
    {
        final Segments3dView< TableRowImageSegment > segments3dView = views.getSegments3dView();

        segments3dView.setObjectsName( sam.metadata().imageId );
        segments3dView.setSegmentFocusZoomLevel( 0.1 );
        segments3dView.setMaxNumSegmentVoxels( 100 * 100 * 100 );

        if ( sam.metadata().imageId.contains( "nuclei" ) )
        {
            //segments3dView.setVoxelSpacing3DView( voxelSpacing3DView );
            segments3dView.setMeshSmoothingIterations( meshSmoothingIterations );
            segments3dView.setSegmentFocusDxyMin( 0 );
            segments3dView.setSegmentFocusDzMin( 0 );
            segments3dView.setTransparency( 0.0 );
        }
        else if ( sam.metadata().imageId.contains( "cells" ) )
        {
            //segments3dView.setVoxelSpacing3DView( voxelSpacing3DView );
            segments3dView.setMeshSmoothingIterations( meshSmoothingIterations );
            segments3dView.setSegmentFocusDxyMin( 0 );
            segments3dView.setSegmentFocusDzMin( 0 );
            segments3dView.setTransparency( 0.4 );
        }
        else if ( sam.metadata().imageId.contains( "chromatin" ) )
        {
            //segments3dView.setVoxelSpacing3DView( voxelSpacing3DView );
            segments3dView.setMeshSmoothingIterations( meshSmoothingIterations );
            segments3dView.setSegmentFocusDxyMin( 0 );
            segments3dView.setSegmentFocusDzMin( 0 );
            segments3dView.setTransparency( 0.6 );
        }
        else
        {
            segments3dView.setMeshSmoothingIterations( meshSmoothingIterations );
            segments3dView.setSegmentFocusDxyMin( 0 );
            segments3dView.setSegmentFocusDzMin( 0 );
            segments3dView.setTransparency( 0.3 );
        }

        segments3dView.setVoxelSpacing( sam.metadata().resolution3dView );
        segments3dView.showSelectedSegments( sam.metadata().showSelectedSegmentsIn3d, true );
    }

    public Map< String, SegmentsTableBdvAnd3dViews > getSourceNameToLabelViews()
    {
        return sourceNameToLabelViews;
    }

    private DefaultImageSourcesModel createLabelsSourceModel(
            SourceAndMetadata< ? > labelsSAM )
    {
        final DefaultImageSourcesModel imageSourcesModel
                = new DefaultImageSourcesModel( false );

        imageSourcesModel.addSourceAndMetadata(
                labelsSAM.metadata().imageId,
                labelsSAM );

        return imageSourcesModel;
    }

    public void removeSourceFromViewers( SourceAndMetadata< ? > sam )
    {
        sam.metadata().showImageIn3d = false;
        sam.metadata().showSelectedSegmentsIn3d = false;
        updateSegments3dView( sam, this );
        updateSource3dView( sam, this, false );

		removeLabelViews( sam.metadata().displayName );
		sourceNameToSourceAndCurrentMetadata.remove( sam.metadata().displayName );

		sam.metadata().bdvStackSource.removeFromBdv();

        for ( SourceAndMetadataChangedListener listener : listeners )
        {
            listener.removedFromBDV( sam );
        }

		if ( sam.metadata().content != null )
        {
            universe.removeContent( sam.metadata().content.getName() );
        }
    }

	private void removeLabelViews( String sourceName )
	{
		if ( sourceNameToLabelViews.keySet().contains( sourceName ) )
        {
            // TODO work more on closing the views properly (also free the memory)
			sourceNameToLabelViews.get( sourceName ).getTableRowsTableView().close();
            sourceNameToLabelViews.get( sourceName ).getSegmentsBdvView().close();
            sourceNameToLabelViews.remove( sourceName );
        }
	}

    public void removeAllSourcesFromViewers()
    {
        final Set< String > visibleSourceNames = getVisibleSourceNames();

        for ( String visibleSourceName : visibleSourceNames )
        {
            final SourceAndMetadata< ? > sam = getSourceAndCurrentMetadata( visibleSourceName );
            removeSourceFromViewers( sam );
        }
    }

    public BdvHandle getBdv()
    {
        return bdv;
    }

    public List< SourceAndMetadataChangedListener > listeners()
    {
        return listeners;
    }
}