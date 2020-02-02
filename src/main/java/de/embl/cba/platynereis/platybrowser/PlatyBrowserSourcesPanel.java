package de.embl.cba.platynereis.platybrowser;

import bdv.util.*;
import bdv.viewer.Interpolation;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.sources.ARGBConvertedRealSource;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.platynereis.Constants;
import de.embl.cba.platynereis.Globals;
import de.embl.cba.platynereis.PlatynereisImageSourcesModel;
import de.embl.cba.platynereis.utils.FileUtils;
import de.embl.cba.platynereis.utils.Utils;
import de.embl.cba.tables.color.LazyLabelsARGBConverter;
import de.embl.cba.tables.ij3d.UniverseUtils;
import de.embl.cba.tables.image.DefaultImageSourcesModel;
import de.embl.cba.tables.image.ImageSourcesModel;
import de.embl.cba.tables.image.SourceAndMetadata;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import de.embl.cba.tables.view.Segments3dView;
import de.embl.cba.tables.view.SegmentsBdvView;
import de.embl.cba.tables.view.TableRowsTableView;
import de.embl.cba.tables.view.combined.SegmentsTableBdvAnd3dViews;
import ij3d.Content;
import ij3d.ContentConstants;
import ij3d.Image3DUniverse;
import mpicbg.spim.data.SpimData;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.vecmath.Color3f;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static de.embl.cba.platynereis.platybrowser.PlatyBrowserUtils.createAnnotatedImageSegmentsFromTableFile;

public class PlatyBrowserSourcesPanel extends JPanel
{
    private final Map< String, SegmentsTableBdvAnd3dViews > sourceNameToLabelsViews;
    //    public List< Color > colors;
    protected Map< String, JPanel > sourceNameToPanel;
    private BdvHandle bdv;
    private final ImageSourcesModel imageSourcesModel;
    private Image3DUniverse universe;
    private int meshSmoothingIterations;
    private double voxelSpacing3DView;
    private SegmentsTableBdvAnd3dViews views;
    private boolean isBdvShownFirstTime = true;

    public PlatyBrowserSourcesPanel( String version,
                                     String imageDataLocation,
                                     String tableDataLocation )
    {
        // TODO
        imageDataLocation = FileUtils.combinePath( imageDataLocation, version );
        tableDataLocation = FileUtils.combinePath( tableDataLocation, version, "tables" );

        Utils.log( "");
        Utils.log( "# Fetching data");
        Utils.log( "Fetching image data from: " + imageDataLocation );
        Utils.log( "Fetching table data from: " + tableDataLocation );

        imageSourcesModel = new PlatynereisImageSourcesModel(
                imageDataLocation,
                tableDataLocation );

        sourceNameToPanel = new LinkedHashMap<>();
        sourceNameToLabelsViews = new LinkedHashMap<>();

        voxelSpacing3DView = 0.05;
        meshSmoothingIterations = 5;

        configPanel();
//        initColors();
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
        sam.metadata().displayColor = color;

        sam.metadata().bdvStackSource.setColor( BdvUtils.asArgbType( sam.metadata().displayColor ) );

        if ( sam.metadata().content != null )
            sam.metadata().content.setColor( new Color3f( sam.metadata().displayColor ));

        if ( sourceNameToLabelsViews.containsKey( sam.metadata().displayName ) )
        {
            final SegmentsBdvView< TableRowImageSegment > segmentsBdvView
                    = sourceNameToLabelsViews.get( sam.metadata().displayName ).getSegmentsBdvView();

            segmentsBdvView.setLabelSourceSingleColor( BdvUtils.asArgbType( sam.metadata().displayColor ) );
        }

        panel.setBackground( sam.metadata().displayColor );
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

        for ( SegmentsTableBdvAnd3dViews views : sourceNameToLabelsViews.values() )
            views.getSegments3dView().setMeshSmoothingIterations( meshSmoothingIterations );
    }

    public double getVoxelSpacing3DView()
    {
        return voxelSpacing3DView;
    }

    public void setVoxelSpacing3DView( double voxelSpacing3DView )
    {
        this.voxelSpacing3DView = voxelSpacing3DView;

        for ( SegmentsTableBdvAnd3dViews views : sourceNameToLabelsViews.values() )
            views.getSegments3dView().setVoxelSpacing3DView( voxelSpacing3DView );
    }

    private void addSourceToVolumeViewer( SourceAndMetadata< ? > sourceAndMetadata )
    {
        if ( ! Globals.showVolumesIn3D.get() ) return;

        if ( universe == null ) init3DUniverse();

        UniverseUtils.showUniverseWindow( universe, bdv.getViewerPanel() );

        DisplaySettings3DViewer settings = getDisplaySettings3DViewer( sourceAndMetadata );

        final Content content = UniverseUtils.addSourceToUniverse(
                universe,
                sourceAndMetadata.source(),
                200 * 200 * 200,
                settings.displayMode,
                settings.color,
                settings.transparency,
                0,
                settings.max
        );

        sourceAndMetadata.metadata().content = content;
    }

	private void init3DUniverse()
	{
		universe = new Image3DUniverse();
	}

	class DisplaySettings3DViewer
    {
        int displayMode;
        float transparency;
        int max;
        ARGBType color = new ARGBType( 0xffffffff );
    }

    private DisplaySettings3DViewer getDisplaySettings3DViewer( SourceAndMetadata< ? > sourceAndMetadata )
    {
        final DisplaySettings3DViewer settings = new DisplaySettings3DViewer();
        if ( sourceAndMetadata.metadata().displayName.contains( Constants.MED )
                || sourceAndMetadata.metadata().displayName.contains( Constants.SEGMENTED )  )
        {
            settings.displayMode = ContentConstants.SURFACE;
            settings.max = 1;
            settings.transparency = 0.3F;
        }
        else if ( sourceAndMetadata.metadata().displayName.contains( Constants.SPM ) )
        {
            settings.displayMode = ContentConstants.VOLUME;
            settings.max = 65535;
            settings.transparency = 0.3F;
        }
        else if ( sourceAndMetadata.metadata().displayName.contains( Constants.EM_FILE_ID ) )
        {
            settings.displayMode = ContentConstants.ORTHO;
            settings.max = 255;
            settings.transparency = 0.0F;
        }
        else
        {

        }

        return settings;
    }

    public void addSourceToPanelAndViewer( String sourceName )
    {
        if ( ! getSourceNames().contains( sourceName ) )
        {
            System.err.println( "Source not present: " + sourceName );
            return;
        }

        final SourceAndMetadata< ? > sourceAndMetadata = getSourceAndMetadata( sourceName );

        addSourceToPanelAndViewer( sourceAndMetadata );
    }

    public SourceAndMetadata< ? > getSourceAndMetadata( String sourceName )
    {
        return imageSourcesModel.sources().get( sourceName );
    }

    public ArrayList< String > getSourceNames()
    {
        return new ArrayList<>( imageSourcesModel.sources().keySet() );
    }

    public ImageSourcesModel getImageSourcesModel()
    {
        return imageSourcesModel;
    }

    private void configPanel()
    {
        this.setLayout( new BoxLayout(this, BoxLayout.Y_AXIS ) );
        this.setAlignmentX( Component.LEFT_ALIGNMENT );
    }

    private void addSourceToPanelAndViewer( SourceAndMetadata< ? > sourceAndMetadata )
    {
        if ( sourceNameToPanel.containsKey(  sourceAndMetadata.metadata().displayName ) )
            return;

        addSourceToViewer( sourceAndMetadata );
        new Thread( () -> addSourceToVolumeViewer( sourceAndMetadata ) ).start();
        addSourceToPanel( sourceAndMetadata );
    }

    private void addSourceToViewer( SourceAndMetadata< ? > sam )
    {
        Prefs.showScaleBar( true );
        Prefs.showMultibox( false );

        final Metadata metadata = sam.metadata();

        if ( metadata.modality == Metadata.Modality.Segmentation )
        {
            if ( ! showAnnotatedLabelsSource( sam ) )
            {
                // fall back on just showing the image
                // without annotations
                showLabelsSource( sam );
            }
        }
        else
        {
            showIntensitySource( sam );
        }

        if ( isBdvShownFirstTime )
        {
            BdvUtils.centerBdvWindowLocation( bdv );
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

        bdvStackSource.setDisplayRange(
                metadata.displayRangeMin,
                metadata.displayRangeMax );

        bdvStackSource.setColor( Utils.asArgbType( metadata.displayColor ) );

        bdv = bdvStackSource.getBdvHandle();

        metadata.bdvStackSource = bdvStackSource;
    }

    private void showLabelsSource( SourceAndMetadata< ? > sam )
    {
        final ARGBConvertedRealSource source =
                new ARGBConvertedRealSource( sam.source(),
                    new LazyLabelsARGBConverter() );

        sam.metadata().bdvStackSource = BdvFunctions.show(
                source,
                1,
                BdvOptions.options().addTo( bdv ) );

        sam.metadata().bdvStackSource.setDisplayRange( 0, 1000 );
    }

    private boolean showAnnotatedLabelsSource( SourceAndMetadata< ? > sam )
    {
        if ( sam.metadata().segmentsTablePath == null ) return false;

        try
        {
            final List< TableRowImageSegment > segments
                    = createAnnotatedImageSegmentsFromTableFile(
                         sam.metadata().segmentsTablePath,
                         sam.metadata().imageId );

            // TODO: this is not logical anymore, because there can be several views for different segments...
            views = new SegmentsTableBdvAnd3dViews(
                    segments,
                    createLabelsSourceModel( sam ),
                    sam.metadata().imageId,
                    bdv,
                    universe );

            configureSegmentsView( sam );
            configureTableView( sam );

            // update bdv in case this is was first source to be shown.
            bdv = views.getSegmentsBdvView().getBdv();

            // keep bdvStackSource handle, for changing its color, visibility, a.s.o.
            sam.metadata().bdvStackSource = views
                    .getSegmentsBdvView()
                    .getCurrentSources().get( 0 )
                    .metadata().bdvStackSource;

            sam.metadata().bdvStackSource.setDisplayRange( 0, 1000 );

            sourceNameToLabelsViews.put( sam.metadata().displayName, views );
        }
        catch ( Exception e )
        {
            Utils.log( "" );
            Utils.log( "Could not find or open segments table: "
                    + sam.metadata().segmentsTablePath);
            Utils.log( "" );

            e.printStackTrace();

            return false;
        }

        return true;
    }

    private void configureTableView( SourceAndMetadata< ? > sam )
    {
        final TableRowsTableView< TableRowImageSegment > tableRowsTableView = views.getTableRowsTableView();

        final String tablesLocation = FileUtils.getParentLocation( sam.metadata().segmentsTablePath );

        tableRowsTableView.setTablesDirectory( tablesLocation );
        tableRowsTableView.setMergeByColumnName( Globals.COLUMN_NAME_SEGMENT_LABEL_ID );
    }

    private void configureSegmentsView( SourceAndMetadata< ? > sam )
    {
        final Segments3dView< TableRowImageSegment > segments3dView
                = views.getSegments3dView();

        segments3dView.setShowSegments( Globals.showSegmentsIn3D );
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

    public SegmentsTableBdvAnd3dViews getViews()
    {
        return views;
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
        panel.setOpaque( true );
        panel.setBackground( metadata.displayColor );

        JLabel sourceNameLabel = new JLabel( sourceName );
        sourceNameLabel.setHorizontalAlignment( SwingConstants.CENTER );

        int[] buttonDimensions = new int[]{ 50, 30 };

        panel.add( sourceNameLabel );

        addColorButton( panel, buttonDimensions, sam );

        final JButton brightnessButton =
                SourcesDisplayUI.createBrightnessButton(
                        buttonDimensions, sam,
                        0.0, 65535.0);

        final JButton removeButton =
                createRemoveButton( sam, buttonDimensions );

        final JCheckBox sliceViewVisibilityCheckbox =
                SourcesDisplayUI.createSliceViewVisibilityCheckbox( buttonDimensions, sam, true );

        final JCheckBox volumeVisibilityCheckbox =
                SourcesDisplayUI.createVolumeViewVisibilityCheckbox( buttonDimensions, sam, true );


        panel.add( brightnessButton );
        panel.add( removeButton );
        panel.add( volumeVisibilityCheckbox );
        panel.add( sliceViewVisibilityCheckbox );

        add( panel );
        refreshGui();

        sourceNameToPanel.put( sourceName, panel );
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
                        removeSourceFromPanelAndViewers(
                                sam.metadata().displayName,
                                sam.metadata().bdvStackSource,
                                sam.metadata().content );

                    }
                } );


        return removeButton;
    }

    private void removeSourceFromPanelAndViewers(
            String sourceName,
            BdvStackSource bdvStackSource,
            Content content )
    {
		removeSourceFromPanel( sourceName );

		removeLabelsViews( sourceName );

		BdvUtils.removeSource( bdv, bdvStackSource );

		if ( content != null ) universe.removeContent( content.getName() );

        refreshGui();
    }

	private void removeLabelsViews( String sourceName )
	{
		if ( sourceNameToLabelsViews.keySet().contains( sourceName ) )
        {
			sourceNameToLabelsViews.get( sourceName ).close();
			sourceNameToLabelsViews.remove( sourceName );
        }
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