package de.embl.cba.platynereis;

import bdv.util.Bdv;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainUI extends JPanel
{
	final Bdv bdv;
	JFrame frame;
	private double zoom;
	MainCommand mainCommand;

	public MainUI( Bdv bdv, MainCommand mainCommand )
	{
		this.bdv = bdv;
		this.mainCommand = mainCommand;
		zoom = 10.0;

		addSourceSelectionUI( this );
		addPositionUI( this );

		launchUI();
	}

	private void addSourceSelectionUI(JPanel panel )
	{
		final JPanel horizontalLayoutPanel = horizontalLayoutPanel();

		horizontalLayoutPanel.add( new JLabel( "Add source to viewer: " ) );

		final JComboBox dataSources = new JComboBox();

		for ( String name : mainCommand.dataSourcesMap.keySet() )
		{
			dataSources.addItem( name );
		}

		dataSources.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				mainCommand.addDataSourceToBdv( (String) dataSources.getSelectedItem() );
			}
		} );
	}

	private void addPositionUI( JPanel panel )
	{
		final JPanel horizontalLayoutPanel = horizontalLayoutPanel();

		horizontalLayoutPanel.add( new JLabel( "Move to position [x,y,z]" ) );

		final JTextField position = new JTextField( "100.0,100.0,100.0" );

		position.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				moveToPosition( Utils.delimitedStringToDoubleArray( position.getText(), ",") );
			}
		} );

		horizontalLayoutPanel.add( position );

		panel.add( horizontalLayoutPanel );
	}

	private void moveToPosition( double[] position )
	{
		double[] positionInViewer = new double[ 3 ];

		ProSPrRegistration.getTransformationFromEmToProsprInMicrometerUnits().apply( position, positionInViewer );

		Utils.centerBdvViewToPosition( positionInViewer, zoom, bdv );
	}

	private JPanel horizontalLayoutPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout(panel, BoxLayout.LINE_AXIS) );
		panel.setBorder( BorderFactory.createEmptyBorder(0, 10, 10, 10) );
		panel.add( Box.createHorizontalGlue() );
		return panel;
	}

	private void launchUI()
	{
		(new Thread(new Runnable(){
			public void run(){
				createAndShowUI();
			}
		})).start();
	}

	/**
	 * Create the GUI and show it.  For thread safety,
	 * this method should be invoked from the
	 * event-dispatching thread.
	 */
	private void createAndShowUI( )
	{

		//Create and set up the window.
		frame = new JFrame( "Platynereis Viewer" );
		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

		//Create and set up the content pane.
		setOpaque( true ); //content panes must be opaque
		setLayout( new BoxLayout(this, BoxLayout.Y_AXIS ) );

		frame.setContentPane( this );

		//Display the window.
		frame.pack();
		frame.setVisible( true );
	}

	private void refreshUI()
	{
		this.revalidate();
		this.repaint();
		frame.pack();
	}

}
