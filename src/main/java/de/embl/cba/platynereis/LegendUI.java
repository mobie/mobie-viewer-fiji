package de.embl.cba.platynereis;

import ij.gui.GenericDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class LegendUI extends JPanel implements ActionListener
{
    public static final String CHANGE_COLOR = "Change color";
    public static final String ADAPT_BRIGHTNESS = "Adapt brightness";
    public static final String REMOVE = "Remove";
    public static final String CANCELLED = "Cancelled";
    public static final String COLOR_ACTION = "Color___";
    public static final String BRIGHTNESS_ACTION = "B___";
    public static final String REMOVE_ACTION = "X___";

    public final ArrayList< Color > colors;

    protected Map< String, JPanel > panels;
    JFrame frame;
    final MainCommand mainCommand;

    public LegendUI( MainCommand mainCommand )
    {
        this.mainCommand = mainCommand;
        panels = new LinkedHashMap<>(  );
        colors = getColors();
        createGUI();
    }

    private ArrayList<Color> getColors()
    {
        ArrayList< Color > colors = new ArrayList<>(  );

        colors.add( Color.MAGENTA );
        colors.add( Color.GREEN );
        colors.add( Color.ORANGE );
        colors.add( Color.CYAN );
        colors.add( Color.YELLOW );

        return colors;
    }


    public void addSource( PlatynereisDataSource dataSource )
    {

        if( ! panels.containsKey( dataSource.name ) )
        {

            int[] buttonDimensions = new int[]{ 40, 40 };

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
            panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
            panel.add(Box.createHorizontalGlue());
            panel.setOpaque( true );
            panel.setBackground( dataSource.color );

            JLabel jLabel = new JLabel( dataSource.name );
            jLabel.setHorizontalAlignment( SwingConstants.CENTER );

            JButton color = new JButton( "C" );
            color.addActionListener( this );
            color.setPreferredSize( new Dimension( buttonDimensions[ 0 ],buttonDimensions[ 1 ] ) );
            color.setName( COLOR_ACTION + dataSource.name );

            JButton brightness = new JButton( "B" );
            brightness.addActionListener( this );
            brightness.setPreferredSize( new Dimension( buttonDimensions[ 0 ],buttonDimensions[ 1 ] ) );
            brightness.setName( BRIGHTNESS_ACTION + dataSource.name );

            JButton remove = new JButton( "X" );
            remove.addActionListener( this );
            remove.setPreferredSize( new Dimension( buttonDimensions[ 0 ],buttonDimensions[ 1 ] ) );
            remove.setName( REMOVE_ACTION + dataSource.name );

            panel.add( jLabel );
            panel.add( color );
            panel.add( brightness );
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


    private void removeSource( String name )
    {
        remove( panels.get( name ) );
        panels.remove( name );
        refreshGui();
    }

    private void refreshGui()
    {
        this.revalidate();
        this.repaint();
        frame.pack();
    }

    @Override
    public void actionPerformed( ActionEvent e )
    {

        JButton button = (JButton) e.getSource();
        String name = button.getName().trim();

        if ( name.contains( COLOR_ACTION ))
        {
            String dataSourceName = name.replace( COLOR_ACTION, "" );
            changeColorViaUI( dataSourceName );
        }
        else if ( name.contains( BRIGHTNESS_ACTION ) )
        {
            String dataSourceName = name.replace( BRIGHTNESS_ACTION, "" );
            mainCommand.setBrightness( dataSourceName );
        }
        else if( name.contains( REMOVE_ACTION ) )
        {
            String dataSourceName = name.replace( REMOVE_ACTION, "" );
            mainCommand.hideDataSource( dataSourceName );
            removeSource( dataSourceName );
        }
    }

    public void  showActionUI( String dataSourceName )
    {
        String action = getActionFromUI();

        switch ( action )
        {
            case REMOVE:
                mainCommand.hideDataSource( dataSourceName );
                removeSource( dataSourceName );
                break;
            case CHANGE_COLOR:
                changeColorViaUI( dataSourceName );
                break;
            case ADAPT_BRIGHTNESS:
                mainCommand.setBrightness( dataSourceName );
                break;
            case CANCELLED:
                break;
        }

    }

    private String getActionFromUI()
    {
        GenericDialog genericDialog = new GenericDialog( "Choose action " );

        genericDialog.addChoice( "Action", new String[]
                {
                        CHANGE_COLOR,
                        ADAPT_BRIGHTNESS,
                        REMOVE
                },
                CHANGE_COLOR);

        genericDialog.showDialog();

        if ( genericDialog.wasCanceled() ) return CANCELLED;

        return genericDialog.getNextChoice();
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


    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private void createGUI( )
    {

        //Create and set up the window.
        frame = new JFrame( "" );
        frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

        //Create and set up the content pane.
        setOpaque( true ); //content panes must be opaque
        setLayout( new BoxLayout(this, BoxLayout.Y_AXIS ) );

        frame.setContentPane( this );

        //Display the window.
        frame.pack();
        frame.setVisible( true );
    }


}