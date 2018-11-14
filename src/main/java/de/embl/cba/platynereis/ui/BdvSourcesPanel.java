package de.embl.cba.platynereis.ui;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.Bdv;
import de.embl.cba.bdv.utils.BdvUserInterfaceUtils;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.platynereis.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static de.embl.cba.bdv.utils.BdvUserInterfaceUtils.*;
import static de.embl.cba.bdv.utils.BdvUserInterfaceUtils.addSourcesDisplaySettingsUI;
import static de.embl.cba.bdv.utils.BdvUserInterfaceUtils.createColorButton;
import static de.embl.cba.platynereis.Utils.asArgbType;

public class BdvSourcesPanel extends JPanel implements ActionListener
{
    public static final String CHANGE_COLOR = "Change color";
    public static final String ADAPT_BRIGHTNESS = "Adapt brightness";
    public static final String REMOVE = "Remove";
    public static final String CANCELLED = "Cancelled";
    public static final String COLOR_ACTION = "C___";
    public static final String BRIGHTNESS_ACTION = "B___";
    public static final String TOGGLE_ACTION = "T___";
    public static final String REMOVE_ACTION = "X___";

    public ArrayList< Color > colors;

    protected Map< String, JPanel > panels;
    JFrame frame;
    final MainFrame mainFrame;
    final MainCommand mainCommand;
    final Bdv bdv;
    private final Map< String, PlatynereisDataSource > dataSources;


    public BdvSourcesPanel( MainFrame mainFrame, Bdv bdv, MainCommand mainCommand )
    {
        this.mainFrame = mainFrame;
        this.bdv = bdv;
        this.mainCommand = mainCommand;
        this.dataSources = mainCommand.dataSources;
        this.setLayout( new BoxLayout(this, BoxLayout.Y_AXIS ) );
        this.setAlignmentX(Component.LEFT_ALIGNMENT);
        panels = new LinkedHashMap<>(  );
        this.addSourceToPanelAndViewer( dataSources.get( mainCommand.getEmRawDataName() ).name );
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

    private Color getColor( PlatynereisDataSource dataSource )
    {
        if ( dataSource.name.contains( Constants.EM_FILE_ID ) )
        {
            return Color.WHITE;
        }
        else if ( panels.size() <= colors.size()  & panels.size() > 0 )
        {
            return colors.get( panels.size() - 1 );
        }
        else
        {
            return colors.get( 0 );
        }
    }

    public void addSourceToPanelAndViewer( String name )
    {
        PlatynereisDataSource source = dataSources.get( name );

        addSourceToViewer( source );

        addSourceToPanel( source );

    }

    private void addSourceToViewer( PlatynereisDataSource source )
    {

        if ( source.bdvSource == null || source.bdvSource.getBdvHandle() == null )
        {
            switch ( Constants.BDV_XML_SUFFIX ) // TODO: makes no sense...
            {
                case ".tif":
                    Utils.loadAndShowSourceFromTiffFile( source, bdv  );
                    break;
                case ".xml":
                    if ( source.spimData == null && ! source.isLabelSource )
                    {
                        source.spimData = Utils.openSpimData( source.file );
                    }
                    Utils.showSourceInBdv( source, bdv  );
                    break;
                default:
                    Utils.log( "Unsupported format: " + Constants.BDV_XML_SUFFIX );
            }
        }

        if ( ! source.isLabelSource )
        {
            source.color = getColor( source );
            source.bdvSource.setColor( asArgbType( source.color ) );
        }

        source.bdvSource.setActive( true );
        source.isActive = true;
    }

    public void addSourceToPanel( String name )
    {
        addSourceToPanel ( dataSources.get( name ) );
    }


    public void addSourceToPanel( PlatynereisDataSource dataSource )
    {

        if( ! panels.containsKey( dataSource.name ) )
        {
            dataSource.color = getColor( dataSource );

            int[] buttonDimensions = new int[]{ 50, 30 };

            JPanel panel = new JPanel();
            panel.setLayout( new BoxLayout(panel, BoxLayout.LINE_AXIS) );
            panel.setBorder( BorderFactory.createEmptyBorder(0, 10, 0, 10 ) );
            panel.add( Box.createHorizontalGlue() );
            panel.setOpaque( true );
            panel.setBackground( dataSource.color );

            JLabel jLabel = new JLabel( dataSource.name );
            jLabel.setHorizontalAlignment( SwingConstants.CENTER );

            final JButton colorButton = createColorButton( panel, buttonDimensions, dataSource.bdvSource );

            final JButton brightnessButton = createBrightnessButton( buttonDimensions, dataSource.name, dataSource.bdvSource  );

			final JButton toggleButton = createToggleButton( buttonDimensions, dataSource.bdvSource );

            JButton remove = new JButton( "X" );
            remove.addActionListener( new ActionListener()
            {
                @Override
                public void actionPerformed( ActionEvent e )
                {
                    removeSource( dataSource.name );
                }
            } );

            panel.add( jLabel );
            panel.add( colorButton );
            panel.add( brightnessButton );
            panel.add( toggleButton );
            panel.add( remove );

            //Font font = Font.createFont(Font.TRUETYPE_FONT, new FileInputStream("font.ttf"));
            //button.setFont( new Font.createFont( Font.TRUETYPE_FONT ) );

            add( panel );
            panels.put( dataSource.name, panel );

            refreshGui();

        }

    }


    private String padded( String string )
    {
        return string;
        //return String.format("%20s", string);
    }


    private ArrayList< String > getCurrentSourceNames()
    {
        return new ArrayList<>( panels.keySet() );
    }

    public void removeAllProSPrSources()
    {
        final ArrayList< String > names = getCurrentSourceNames();

        for ( String name : names )
        {
            if ( ! name.contains( Constants.EM_FILE_ID ) )
            {
                removeSource( name );
            }
        }
    }

    private void removeSource( String name )
    {
        mainCommand.removeDataSource( name );
        remove( panels.get( name ) );
        panels.remove( name );
        refreshGui();
    }

    private void refreshGui()
    {
        this.revalidate();
        this.repaint();
    }

    @Override
    public void actionPerformed( ActionEvent e )
    {

        // TODO: do this with inline action listeners.

        JButton button = (JButton) e.getSource();
        String name = button.getName().trim();

        if( name.contains( REMOVE_ACTION ) )
        {
            String dataSourceName = name.replace( REMOVE_ACTION, "" );
            removeSource( dataSourceName );
        }
    }


    public void toggleVisibility( String name )
    {
        final PlatynereisDataSource source = dataSources.get( name );

        if ( source.bdvSource == null || source.bdvSource.getBdvHandle() == null )
        {
            addSourceToViewer( source );
        }
        else
        {
            boolean isActive = source.isActive;
            source.isActive = !isActive;
            source.bdvSource.setActive( !isActive );
        }

    }

    public void changeColorViaUI( String name )
    {
        Color color = JColorChooser.showDialog( null, "Select color for " + name, null );

        if ( color != null )
        {
            mainCommand.setDataSourceColor( name, color );
            panels.get( name ).setBackground( color );
        }

    }



}