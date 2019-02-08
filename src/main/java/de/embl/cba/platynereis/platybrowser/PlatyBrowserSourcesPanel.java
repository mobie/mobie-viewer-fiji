package de.embl.cba.platynereis.platybrowser;

import bdv.util.BdvStackSource;
import de.embl.cba.tables.modelview.images.Metadata;
import de.embl.cba.tables.modelview.images.SourceAndMetadata;
import de.embl.cba.tables.modelview.views.bdv.ImageSegmentsBdvView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static de.embl.cba.bdv.utils.BdvUserInterfaceUtils.*;

public class PlatyBrowserSourcesPanel extends JPanel
{
    private final ImageSegmentsBdvView bdvView;

    public List< Color > colors;
    protected Map< String, JPanel > sourceNameToPanel;

    public PlatyBrowserSourcesPanel( ImageSegmentsBdvView bdvView )
    {
        this.bdvView = bdvView;
        this.setLayout( new BoxLayout(this, BoxLayout.Y_AXIS ) );
        this.setAlignmentX( Component.LEFT_ALIGNMENT );
        sourceNameToPanel = new LinkedHashMap<>(  );
        initColors();
    }

    private void initColors()
    {
        colors = new ArrayList<>(  );

        colors.add( Color.YELLOW );
        colors.add( Color.MAGENTA );
        colors.add( Color.CYAN );
        colors.add( Color.BLUE );
        colors.add( Color.ORANGE );
        colors.add( Color.GREEN );
        colors.add( Color.PINK );

    }

    private Color getColor( Metadata metadata )
    {
        if ( metadata.containsKey( Metadata.COLOR ) )
        {
            return ( Color ) metadata.get( Metadata.COLOR );
        }
        else
        {
            return Color.WHITE;
        }
//        else if ( sourceNameToPanel.size() <= colors.size()  & sourceNameToPanel.size() > 0 )
//        {
//            return colors.get( sourceNameToPanel.size() - 1 );
//        }
//        else
//        {
//            return colors.get( 0 );
//        }
    }

    public void addSourceToPanel( SourceAndMetadata sourceAndMetadata, BdvStackSource bdvStackSource )
    {
        final Metadata metadata = sourceAndMetadata.metadata();
        final String sourceName = ( String ) metadata.get( Metadata.DISPLAY_NAME );

        if( ! sourceNameToPanel.containsKey( sourceName ) )
        {
            JPanel panel = new JPanel();
            sourceNameToPanel.put( sourceName, panel );

            panel.setLayout( new BoxLayout(panel, BoxLayout.LINE_AXIS) );
            panel.setBorder( BorderFactory.createEmptyBorder(0, 10, 0, 10 ) );
            panel.add( Box.createHorizontalGlue() );
            panel.setOpaque( true );
            panel.setBackground( getColor( metadata ) );

            JLabel jLabel = new JLabel( sourceName );
            jLabel.setHorizontalAlignment( SwingConstants.CENTER );

            int[] buttonDimensions = new int[]{ 50, 30 };

            final JButton colorButton = createColorButton( panel, buttonDimensions, bdvStackSource );
            final JButton brightnessButton = createBrightnessButton( buttonDimensions, sourceName, bdvStackSource );
            final JButton removeButton = createRemoveButton( sourceAndMetadata, bdvStackSource, buttonDimensions );
            final JCheckBox visibilityCheckbox = createVisibilityCheckbox( buttonDimensions, bdvStackSource, true );

            panel.add( jLabel );
            panel.add( colorButton );
            panel.add( brightnessButton );
            panel.add( removeButton );
            panel.add( visibilityCheckbox );

            add( panel );
            refreshGui();
        }
    }

    private JButton createRemoveButton(
            SourceAndMetadata sourceAndMetadata,
            BdvStackSource bdvStackSource,
            int[] buttonDimensions )
    {
        JButton removeButton = new JButton( "X" );
        removeButton.setPreferredSize( new Dimension( buttonDimensions[ 0 ], buttonDimensions[ 1 ] ) );

        removeButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				removeSource( ( String ) sourceAndMetadata.metadata().get( Metadata.DISPLAY_NAME ), bdvStackSource );
			}
		} );

        return removeButton;
    }

//    public ArrayList< String > getCurrentSourceNames()
//    {
//        return new ArrayList<>( sourceNameToPanel.keySet() );
//    }

//    public void removeAllProSPrSources()
//    {
//        final ArrayList< String > names = getCurrentSourceNames();
//
//        for ( String name : names )
//        {
//            if ( ! name.contains( Constants.EM_FILE_ID ) )
//            {
//                removeSource( name );
//            }
//        }
//    }

    private void removeSource( String sourceName, BdvStackSource bdvStackSource )
    {
        bdvView.removeSingleSource( bdvStackSource );
        remove( sourceNameToPanel.get( sourceName ) );
        sourceNameToPanel.remove( sourceName );
        refreshGui();
    }

    private void refreshGui()
    {
        this.revalidate();
        this.repaint();
    }

}