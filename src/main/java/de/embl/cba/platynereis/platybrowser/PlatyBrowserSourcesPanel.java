package de.embl.cba.platynereis.platybrowser;

import bdv.util.*;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.sources.ARGBConvertedRealSource;
import de.embl.cba.platynereis.Globals;
import de.embl.cba.platynereis.PlatynereisImageSourcesModel;
import de.embl.cba.platynereis.utils.Utils;
import de.embl.cba.tables.color.LazyLabelsARGBConverter;
import de.embl.cba.tables.image.DefaultImageSourcesModel;
import de.embl.cba.tables.image.ImageSourcesModel;
import de.embl.cba.tables.image.Metadata;
import de.embl.cba.tables.image.SourceAndMetadata;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import de.embl.cba.tables.view.Segments3dView;
import de.embl.cba.tables.view.combined.SegmentsTableBdvAnd3dViews;
import ij3d.Image3DUniverse;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static de.embl.cba.bdv.utils.BdvDialogs.*;
import static de.embl.cba.platynereis.platybrowser.PlatyBrowserUtils.createAnnotatedImageSegmentsFromTableFile;

public class PlatyBrowserSourcesPanel extends JPanel
{
    private final Map< String, SegmentsTableBdvAnd3dViews > sourceNameToLabelsViews;
    //    public List< Color > colors;
    protected Map< String, JPanel > sourceNameToPanel;
    private BdvHandle bdv;
    private final ImageSourcesModel imageSourcesModel;
    private final Image3DUniverse universe;
    private int meshSmoothingIterations;
    private double voxelSpacing3DView;

    public PlatyBrowserSourcesPanel( String imageDataLocation, String tableDataLocation )
    {
        imageSourcesModel = new PlatynereisImageSourcesModel( imageDataLocation, tableDataLocation );
        sourceNameToPanel = new LinkedHashMap<>();
        sourceNameToLabelsViews = new LinkedHashMap<>();
        voxelSpacing3DView = 0.05;
        meshSmoothingIterations = 5;
        universe = new Image3DUniverse();

        configPanel();
//        initColors();
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

    public void addSourceToPanelAndViewer( String sourceName )
    {
        if ( ! getSourceNames().contains( sourceName ) )
        {
            System.err.println( "Source not present: " + sourceName );
            return;
        }

        addSourceToPanelAndViewer( getSourceAndMetadata( sourceName ) );
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

//    private void initColors()
//    {
//        colors = new ArrayList<>(  );
//        colors.add( Color.YELLOW );
//        colors.add( Color.MAGENTA );
//        colors.add( Color.CYAN );
//        colors.add( Color.BLUE );
//        colors.add( Color.ORANGE );
//        colors.add( Color.GREEN );
//        colors.add( Color.PINK );
//    }

    private Color getColor( Metadata metadata )
    {
        return metadata.displayColor;
//        else if ( sourceNameToPanel.size() <= colors.size()  & sourceNameToPanel.size() > 0 )
//        {
//            return colors.get( sourceNameToPanel.size() - 1 );
//        }
//        else
//        {
//            return colors.get( 0 );
//        }
    }



    private void addSourceToPanelAndViewer( SourceAndMetadata< ? > sourceAndMetadata )
    {
        if ( sourceNameToPanel.containsKey(  sourceAndMetadata.metadata().displayName ) )
        {
            // source is already shown, don't add again
            return;
        }

        addSourceToViewer( sourceAndMetadata );
        addSourceToPanel( sourceAndMetadata );
    }

    private void addSourceToViewer( SourceAndMetadata< ? > sam )
    {
        Prefs.showScaleBar( true ); // make sure bdv has a scale bar

        final Metadata metadata = sam.metadata();

        if ( metadata.flavour == Metadata.Flavour.LabelSource )
        {
            if ( metadata.segmentsTablePath != null )
                if ( ! showAnnotatedLabelsSource( sam ) )
                    showLabelsSource( sam );
            else
                showLabelsSource( sam );

            sam.metadata().bdvStackSource.setDisplayRange( 0, 1000 );
        }
        else
        {
            showIntensitySource( sam );
        }
    }

    private void showIntensitySource( SourceAndMetadata< ? > sam )
    {
        final Metadata metadata = sam.metadata();

//        final BdvStackSource bdvStackSource = BdvFunctions.show(
//                sam.source(),
//                1,
//                BdvOptions.options().sourceTransform(
//                        metadata.sourceTransform ).addTo( bdv ) );


        final BdvStackSource bdvStackSource = BdvFunctions.show(
                sam.source(),
                1,
                BdvOptions.options().addTo( bdv ) );

        bdvStackSource.setActive( true );

        bdvStackSource.setDisplayRange(
                metadata.displayRangeMin,
                metadata.displayRangeMax );

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
    }

    private boolean showAnnotatedLabelsSource( SourceAndMetadata< ? > sam )
    {
        try
        {
            final List< TableRowImageSegment > segments
                    = createAnnotatedImageSegmentsFromTableFile(
                    sam.metadata().segmentsTablePath,
                    sam.metadata().imageId );

            final SegmentsTableBdvAnd3dViews views =
                    new SegmentsTableBdvAnd3dViews(
                            segments,
                            createLabelsSourceModel( sam ),
                            sam.metadata().imageId,
                            bdv,
                            universe );

            final Segments3dView< TableRowImageSegment > segments3dView = views.getSegments3dView();
            segments3dView.setShowSegments( Globals.showSegmentsIn3D );

            if ( sam.metadata().imageId.contains( "nuclei" ) )
            {
                segments3dView.setVoxelSpacing3DView( voxelSpacing3DView );
                segments3dView.setMeshSmoothingIterations( meshSmoothingIterations );
                segments3dView.setSegmentFocusDxyMin( 50 );
                segments3dView.setSegmentFocusDzMin( 10000 );
                segments3dView.setTransparency( 0.0 );
                segments3dView.setSegmentFocusZoomLevel( 0.005 );
                segments3dView.setMaxNumBoundingBoxElements( 300 * 300 * 300 );
            }

            if ( sam.metadata().imageId.contains( "cells" ) )
            {
                segments3dView.setVoxelSpacing3DView( voxelSpacing3DView );
                segments3dView.setMeshSmoothingIterations( meshSmoothingIterations );
                segments3dView.setSegmentFocusDxyMin( 300 );
                segments3dView.setSegmentFocusDzMin( 10000 );
                segments3dView.setTransparency( 0.6 );
                segments3dView.setSegmentFocusZoomLevel( 0.005 );
                segments3dView.setMaxNumBoundingBoxElements( 300 * 300 * 300 );
            }


            // update bdv in case this is was first source to be shown.
            bdv = views.getSegmentsBdvView().getBdv();

            // set bdvStackSource field, for changing its color, visibility, a.s.o.
            sam.metadata().bdvStackSource = views
                    .getSegmentsBdvView()
                    .getCurrentSources().get( 0 )
                    .metadata().bdvStackSource;

            sourceNameToLabelsViews.put( sam.metadata().displayName, views );

        }
        catch ( Exception e )
        {
            Utils.log( "" );
            Utils.log( "Could not find or open segments table: " + sam.metadata().segmentsTablePath);
            Utils.log( "" );

            return false;
        }

        return true;
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
        final BdvStackSource bdvStackSource = metadata.bdvStackSource;

        JPanel panel = new JPanel();

        panel.setLayout( new BoxLayout(panel, BoxLayout.LINE_AXIS) );
        panel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 0, 10 ) );
        panel.add( Box.createHorizontalGlue() );
        panel.setOpaque( true );
        panel.setBackground( getColor( metadata ) );

        JLabel sourceNameLabel = new JLabel( sourceName );
        sourceNameLabel.setHorizontalAlignment( SwingConstants.CENTER );

        int[] buttonDimensions = new int[]{ 50, 30 };

        final JButton colorButton =
                createColorButton( panel, buttonDimensions, bdvStackSource );
        final JButton brightnessButton =
                createBrightnessButton( buttonDimensions, sourceName, bdvStackSource,
                        0.0, 65535.0);
        final JButton removeButton =
                createRemoveButton( sam, buttonDimensions );
        final JCheckBox visibilityCheckbox =
                createVisibilityCheckbox( buttonDimensions, bdvStackSource, true );

        panel.add( sourceNameLabel );
        panel.add( colorButton );
        panel.add( brightnessButton );
        panel.add( removeButton );
        panel.add( visibilityCheckbox );

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
                e -> removeSourceFromPanelAndViewer(
                        sam.metadata().displayName, sam.metadata().bdvStackSource ) );

        return removeButton;
    }

    private void removeSourceFromPanelAndViewer(
            String sourceName,
            BdvStackSource bdvStackSource )
    {
		removeSourceFromPanel( sourceName );

		removeLabelsViews( sourceName );

		BdvUtils.removeSource( bdv, bdvStackSource );

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