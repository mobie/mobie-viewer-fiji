package de.embl.cba.platynereis.platybrowser;

import bdv.util.BdvStackSource;
import de.embl.cba.tables.modelview.images.SourceAndMetadata;
import de.embl.cba.tables.modelview.images.SourceMetadata;
import de.embl.cba.tables.modelview.views.ImageSegmentsBdvView;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import static de.embl.cba.bdv.utils.BdvUserInterfaceUtils.*;

public class PlatyBrowserSourcesPanel extends JPanel
{
    private final ImageSegmentsBdvView< ?, ? > bdvView;

    public List< Color > colors;
    protected Map< String, JPanel > sourceNameToPanel;

    public PlatyBrowserSourcesPanel( ImageSegmentsBdvView< ?, ? > bdvView )
    {
        this.bdvView = bdvView;
        this.setLayout( new BoxLayout(this, BoxLayout.Y_AXIS ) );
        this.setAlignmentX( Component.LEFT_ALIGNMENT );
        sourceNameToPanel = new LinkedHashMap<>(  );
        initColors();

        for ( SourceAndMetadata< ? > sourceAndMetadata : bdvView.getCurrentSources() )
        {
            addSourceToPanel( sourceAndMetadata );
        }

        this.bdvView.getCurrentSources();
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

    private Color getColor( SourceMetadata metadata )
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

    public void addSourceToPanel( SourceAndMetadata< ? > sourceAndMetadata )
    {
        final SourceMetadata metadata = sourceAndMetadata.metadata();
        final String sourceName = metadata.displayName;
        final BdvStackSource bdvStackSource = metadata.bdvStackSource;

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

        final JButton colorButton =
                createColorButton( panel, buttonDimensions, bdvStackSource );
        final JButton brightnessButton =
                createBrightnessButton( buttonDimensions, sourceName, bdvStackSource );
        final JButton removeButton =
                createRemoveButton( sourceAndMetadata, bdvStackSource, buttonDimensions );
        final JCheckBox visibilityCheckbox =
                createVisibilityCheckbox( buttonDimensions, bdvStackSource, true );

        panel.add( jLabel );
        panel.add( colorButton );
        panel.add( brightnessButton );
        panel.add( removeButton );
        panel.add( visibilityCheckbox );

        add( panel );
        refreshGui();
    }

    private JButton createRemoveButton(
            SourceAndMetadata sourceAndMetadata,
            BdvStackSource bdvStackSource,
            int[] buttonDimensions )
    {
        JButton removeButton = new JButton( "X" );
        removeButton.setPreferredSize( new Dimension( buttonDimensions[ 0 ], buttonDimensions[ 1 ] ) );

        removeButton.addActionListener(
                e -> removeSource( sourceAndMetadata.metadata().displayName, bdvStackSource ) );

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
        bdvView.removeSource( bdvStackSource );
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