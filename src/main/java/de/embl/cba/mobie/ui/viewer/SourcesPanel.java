package de.embl.cba.mobie.ui.viewer;

import bdv.util.*;
import bdv.viewer.Interpolation;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.bdv.utils.sources.ARGBConvertedRealSource;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.mobie.Constants;
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
import ij3d.Content;
import ij3d.ContentConstants;
import ij3d.Image3DUniverse;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.vecmath.Color3f;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.*;
import java.util.List;

import static de.embl.cba.mobie.utils.Utils.createAnnotatedImageSegmentsFromTableFile;

public class SourcesPanel extends JPanel
{
    private final Map< String, SegmentsTableBdvAnd3dViews > sourceNameToLabelViews;
    private Map< String, JPanel > sourceNameToPanel;
    private Map< String, SourceAndMetadata< ? > > sourceNameToSourceAndMetadata;
    private BdvHandle bdv;
    private final SourcesModel imageSourcesModel;
    private Image3DUniverse universe;
    private int meshSmoothingIterations;
    private double voxelSpacing3DView;
    private boolean isBdvShownFirstTime = true;
    private JFrame jFrame;

    public SourcesPanel( SourcesModel imageSourcesModel )
    {
        this.imageSourcesModel = imageSourcesModel;
        sourceNameToPanel = new LinkedHashMap<>();
        sourceNameToLabelViews = new LinkedHashMap<>();
        sourceNameToSourceAndMetadata = new LinkedHashMap<>();

        voxelSpacing3DView = 0.05;
        meshSmoothingIterations = 5;

        configPanel();
    }

    public static void updateSource3dView( SourceAndMetadata< ? > sam, SourcesPanel sourcesPanel, boolean showImageIn3d )
    {
        sam.metadata().showImageIn3d = showImageIn3d;

        if ( sam.metadata().type.equals( Metadata.Type.Segmentation ) ) return;

        if ( showImageIn3d )
        {
            sourcesPanel.showSourceInVolumeViewer( sam );
        }
        else
        {
            if ( sam.metadata().content != null )
                sam.metadata().content.setVisible( false );
        }
    }

    public static void updateSegments3dView( Metadata metadata, SourcesPanel sourcesPanel, boolean showSelectedSegmentsIn3D )
    {
        if ( metadata.views != null )
        {
            final Segments3dView< TableRowImageSegment > segments3dView = metadata.views.getSegments3dView();

            segments3dView.setShowSelectedSegmentsIn3D( showSelectedSegmentsIn3D );

            if ( showSelectedSegmentsIn3D )
                sourcesPanel.setUniverse( segments3dView.getUniverse() );
        }
    }

    private void addColorButton( JPanel panel, int[] buttonDimensions, SourceAndMetadata< ? > sam )
    {
        JButton colorButton;
        colorButton = new JButton( "C" );

        colorButton.setPreferredSize(
                new Dimension( buttonDimensions[ 0 ], buttonDimensions[ 1 ] ) );

        colorButton.addActionListener( e -> {
            Color color = JColorChooser.showDialog( null, "", null );

            if ( color == null ) return;

            setSourceColor( sam, color, panel );

        } );

        panel.add( colorButton );
    }

    private void setSourceColor( SourceAndMetadata< ? > sam, Color color, JPanel panel )
    {
        sam.metadata().color = color.toString();

        sam.metadata().bdvStackSource.setColor( ColorUtils.getARGBType( color ) );

        if ( sam.metadata().content != null )
            sam.metadata().content.setColor( new Color3f( color ));

        if ( sourceNameToLabelViews.containsKey( sam.metadata().displayName ) )
        {
            final SegmentsBdvView< TableRowImageSegment > segmentsBdvView
                    = sourceNameToLabelViews.get( sam.metadata().displayName ).getSegmentsBdvView();

            segmentsBdvView.setLabelSourceSingleColor( ColorUtils.getARGBType( color ) );
        }

        panel.setBackground( color );
    }

    public void setSourceColor( String sourceName, Color color )
    {
        if ( ! sourceNameToPanel.containsKey( sourceName ) )
        {
            System.err.println( "Source not displayed: " + sourceName );
            return;
        }

        final SourceAndMetadata< ? > sam = getSourceAndMetadata( sourceName );
        final JPanel jPanel = sourceNameToPanel.get( sourceName );

        setSourceColor( sam, color, jPanel );
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

    public double getVoxelSpacing3DView()
    {
        return voxelSpacing3DView;
    }

    public void setVoxelSpacing3DView( double voxelSpacing3DView )
    {
        this.voxelSpacing3DView = voxelSpacing3DView;

        for ( SegmentsTableBdvAnd3dViews views : sourceNameToLabelViews.values() )
            views.getSegments3dView().setVoxelSpacing3DView( voxelSpacing3DView );
    }

    public void showSourceInVolumeViewer( SourceAndMetadata< ? > sam )
    {
    	if ( sam.metadata().showImageIn3d )
        {
            initAndShowUniverseIfNecessary();

            if ( sam.metadata().content == null )
            {
                DisplaySettings3DViewer settings = getDisplaySettings3DViewer( sam );

                final Content content = UniverseUtils.addSourceToUniverse(
                        universe,
                        sam.source(),
                        300 * 300 * 300, // TODO: make adaptable
                        settings.displayMode,
                        settings.color,
                        settings.transparency,
                        0,
                        settings.max
                );

                sam.metadata().content = content;
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

    public void setParentComponent( JFrame jFrame )
    {
        this.jFrame = jFrame;
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

        final ARGBType argbType = ColorUtils.getARGBType( sam.metadata().color );
        if ( argbType != null ) settings.color = argbType;

        return settings;
    }

    public void addSourceToPanelAndViewer( String sourceName )
    {
        if ( ! getSourceNames().contains( sourceName ) )
        {
            Logger.error( "Source not present: " + sourceName );
            return;
        }
        else
        {
            final SourceAndMetadata< ? > sam = getSourceAndMetadata( sourceName );
            sourceNameToSourceAndMetadata.put( sam.metadata().displayName, sam );
            addSourceToPanelAndViewer( sam );
        }
    }

    public SourceAndMetadata< ? > getSourceAndMetadata( String sourceName )
    {
        return imageSourcesModel.sources().get( sourceName );
    }

    public ArrayList< String > getSourceNames()
    {
        return new ArrayList<>( imageSourcesModel.sources().keySet() );
    }

    public Set< String > getVisibleSourceNames()
    {
        return Collections.unmodifiableSet( new HashSet<>( sourceNameToPanel.keySet() ) );
    }

    public SourcesModel getImageSourcesModel()
    {
        return imageSourcesModel;
    }

    private void configPanel()
    {
        this.setLayout( new BoxLayout(this, BoxLayout.Y_AXIS ) );
        this.setAlignmentX( Component.LEFT_ALIGNMENT );
    }

    private void addSourceToPanelAndViewer( SourceAndMetadata< ? > sam )
    {
        final String sourceName = sam.metadata().displayName;

        if ( sourceNameToPanel.containsKey( sourceName ) )
        {
            Logger.log( "Source is already shown: " + sourceName + "" );
            return;
        }
        else
        {
            Logger.log( "Adding source: " + sourceName + "..." );
            sourceNameToSourceAndMetadata.put( sourceName, sam );
            addSourceToViewer( sam );
            addSourceToPanel( sam );
        }
    }

    private void addSourceToViewer( SourceAndMetadata< ? > sam )
    {
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

        if ( isBdvShownFirstTime )
        {
            BdvUtils.getViewerFrame( bdv ).
                    setLocation(
                            jFrame.getLocationOnScreen().x + jFrame.getWidth(),
                            jFrame.getLocationOnScreen().y );

            BdvUtils.getViewerFrame( bdv ).setSize( jFrame.getHeight(), jFrame.getHeight() );

            bdv.getViewerPanel().setInterpolation( Interpolation.NLINEAR );

            isBdvShownFirstTime = false;
        }
    }

    private void showIntensitySource( SourceAndMetadata< ? > sam )
    {
        final Metadata metadata = sam.metadata();

        final BdvStackSource bdvStackSource = BdvFunctions.show(
                sam.source(),
                1,
                BdvOptions.options().addTo( bdv ) );

        bdvStackSource.setActive( true );

        setDisplayRange( bdvStackSource, metadata );

        final ARGBType argbType = ColorUtils.getARGBType( metadata.color );
        if ( argbType != null ) bdvStackSource.setColor( argbType );

        bdv = bdvStackSource.getBdvHandle();

        metadata.bdvStackSource = bdvStackSource;
    }

    private void setDisplayRange( BdvStackSource bdvStackSource, Metadata metadata )
    {
        if ( metadata.contrastLimits != null )
            bdvStackSource.setDisplayRange( metadata.contrastLimits[ 0 ], metadata.contrastLimits[ 1 ] );
    }

    // TODO: probably this should be an own class, part of the table-utils views framework
    private void showLabelsSource( SourceAndMetadata< ? > sam )
    {
        final LazyLabelsARGBConverter lazyLabelsARGBConverter = new LazyLabelsARGBConverter();
        final ARGBConvertedRealSource source =
                new ARGBConvertedRealSource( sam.source(),
                        lazyLabelsARGBConverter );

        sam.metadata().bdvStackSource = BdvFunctions.show(
                source,
                1,
                BdvOptions.options().addTo( bdv ) );

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
        final List< TableRowImageSegment > segments
                = createAnnotatedImageSegmentsFromTableFile(
                     sam.metadata().segmentsTablePath,
                     sam.metadata().imageId );

		setUniverse();

		// TODO: use metadata.color explicitly instead of assuming Glasbey
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
        tableRowsTableView.setMergeByColumnName( Constants.COLUMN_NAME_SEGMENT_LABEL_ID );

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
            views.getTableRowsTableView().colorByColumn(
                        sam.metadata().colorByColumn,
                        sam.metadata().color,
                        sam.metadata().contrastLimits[ 0 ],
                        sam.metadata().contrastLimits[ 1 ] );
        }

    }

    private void mergeDefaultTableWithAdditionalTables( SourceAndMetadata< ? > sam, TableRowsTableView< TableRowImageSegment > tableRowsTableView, String tablesLocation )
    {
        if ( sam.metadata().additionalSegmentTableNames == null ) return;

        for ( String tableName : sam.metadata().additionalSegmentTableNames )
        {
            String newTablePath = FileAndUrlUtils.combinePath( tablesLocation, tableName + ".csv" );

            if ( newTablePath.startsWith( "http" ) )
                newTablePath = FileUtils.resolveTableURL( URI.create( newTablePath ) );

            final Map< String, List< String > > newColumns =
                    TableColumns.openAndOrderNewColumns(
                        tableRowsTableView.getTable(),
                        Constants.COLUMN_NAME_SEGMENT_LABEL_ID,
                        newTablePath );

            newColumns.remove( Constants.COLUMN_NAME_SEGMENT_LABEL_ID );
            tableRowsTableView.addColumns( newColumns );
        }
    }

    private void configureSegments3dView( SegmentsTableBdvAnd3dViews views, SourceAndMetadata< ? > sam )
    {
        final Segments3dView< TableRowImageSegment > segments3dView
                = views.getSegments3dView();

        segments3dView.setShowSelectedSegmentsIn3D( sam.metadata().showSelectedSegmentsIn3d );
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

        segments3dView.setAutoResolutionLevel( true );
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

    private void addSourceToPanel( SourceAndMetadata< ? > sam )
    {
        final Metadata metadata = sam.metadata();
        final String sourceName = metadata.displayName;

        JPanel panel = new JPanel();

        panel.setLayout( new BoxLayout(panel, BoxLayout.LINE_AXIS) );
        panel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 0, 10 ) );
        panel.add( Box.createHorizontalGlue() );
        setPanelColor( metadata, panel );

        JLabel sourceNameLabel = new JLabel( sourceName );
        sourceNameLabel.setHorizontalAlignment( SwingUtilities.CENTER );

        int[] buttonDimensions = new int[]{ 50, 30 };
        int[] viewSelectionDimensions = new int[]{ 50, 30 };

        panel.add( sourceNameLabel );

        addColorButton( panel, buttonDimensions, sam );

        final JButton brightnessButton =
                SourcesDisplayUI.createBrightnessButton(
                        buttonDimensions, sam,
                        0.0, 65535.0);

        final JButton removeButton =
                createRemoveButton( sam, buttonDimensions );

        final JCheckBox sliceViewVisibilityCheckbox =
                SourcesDisplayUI.createBigDataViewerVisibilityCheckbox( viewSelectionDimensions, sam, true );

        final JCheckBox volumeVisibilityCheckbox =
                SourcesDisplayUI.createVolumeViewVisibilityCheckbox(
                        this,
                        viewSelectionDimensions,
                        sam,
                        sam.metadata().showImageIn3d || sam.metadata().showSelectedSegmentsIn3d );

        panel.add( brightnessButton );
        panel.add( removeButton );
        panel.add( volumeVisibilityCheckbox );
        panel.add( sliceViewVisibilityCheckbox );

        add( panel );
        refreshGui();

        sourceNameToPanel.put( sourceName, panel );
    }

    private void setPanelColor( Metadata metadata, JPanel panel )
    {
        final Color color = ColorUtils.getColor( metadata.color );
        if ( color != null )
        {
            panel.setOpaque( true );
            panel.setBackground( color );
        }
    }

    private JButton createRemoveButton(
            SourceAndMetadata sam,
            int[] buttonDimensions )
    {
        JButton removeButton = new JButton( "X" );
        removeButton.setPreferredSize(
                new Dimension( buttonDimensions[ 0 ], buttonDimensions[ 1 ] ) );

        removeButton.addActionListener(
                new ActionListener()
                {
                    @Override
                    public void actionPerformed( ActionEvent e )
                    {
                        removeSourceFromPanelAndViewers( sam );
                    }
                } );

        return removeButton;
    }

    public void removeSourceFromPanelAndViewers( String sourceName )
    {
        removeSourceFromPanelAndViewers( sourceNameToSourceAndMetadata.get( sourceName ) );
    }

    public void removeSourceFromPanelAndViewers( SourceAndMetadata< ? > sam )
    {
        updateSegments3dView( sam.metadata(), this, false );
        updateSource3dView( sam, this, false );

        removeSourceFromPanel( sam.metadata().displayName );
		removeLabelsViews( sam.metadata().displayName );

		BdvUtils.removeSource( bdv, ( BdvStackSource ) sam.metadata().bdvStackSource );

		if ( sam.metadata().content != null ) universe.removeContent( sam.metadata().content.getName() );

        refreshGui();
    }

	private void removeLabelsViews( String sourceName )
	{
		if ( sourceNameToLabelViews.keySet().contains( sourceName ) )
        {
            // TODO work more on closing the views properly (also free the memory)
			sourceNameToLabelViews.get( sourceName ).getTableRowsTableView().close();
			sourceNameToLabelViews.remove( sourceName );
        }
	}

    public void removeAllSourcesFromPanelAndViewers()
    {
        final Set< String > visibleSourceNames = getVisibleSourceNames();

        for ( String visibleSourceName : visibleSourceNames )
            removeSourceFromPanelAndViewers( visibleSourceName );
    }

	private void removeSourceFromPanel( String sourceName )
	{
		remove( sourceNameToPanel.get( sourceName ) );
		sourceNameToPanel.remove( sourceName );
	}

	private void refreshGui()
    {
        this.revalidate();
        this.repaint();
    }

    public BdvHandle getBdv()
    {
        return bdv;
    }
}