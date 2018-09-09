package de.embl.cba.platynereis;

import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.util.Bdv;
import ij.IJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Util;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import static de.embl.cba.platynereis.Utils.openSpimData;

public class MainUI extends JPanel
{
	final Bdv bdv;
	JFrame frame;
	private double zoom;
	MainCommand mainCommand;
	private Behaviours behaviours;

	public MainUI( Bdv bdv, MainCommand mainCommand )
	{
		this.bdv = bdv;
		this.mainCommand = mainCommand;
		zoom = 10.0;
		behaviours = new Behaviours( new InputTriggerConfig() );

		addSourceSelectionUI( this );
		addPositionZoomUI( this );
		addPositionPrintUI();
		addShowMostAbundantGenesUI();

		launchUI();
	}

	public void addPositionPrintUI()
	{

		behaviours.install( bdv.getBdvHandle().getTriggerbindings(), "behaviours" );

		add( new JLabel( "[P] Print position" ) );
		add( new JLabel( " " ) );
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			printCoordinates();
		}, "print pos", "P" );
	}

	private void addShowMostAbundantGenesUI()
	{
		final JPanel panel = horizontalLayoutPanel();
		panel.add( new JLabel( "[X] Show nearby genes" ) );

		panel.add( new JLabel( "Radius: " ) );

		final JTextField geneAbundanceRadius = new JTextField( "5.0" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			showMostAbundantGenes( Double.parseDouble(  geneAbundanceRadius.getText() ) );
		}, "show genes", "X" );


	}

	private void showMostAbundantGenes( double radius )
	{
		double[] micrometerMousePosition = new double[ 3 ];
		getMicrometerMousePosition().setPosition( micrometerMousePosition );

		final Set< String > sources = mainCommand.dataSourcesMap.keySet();

		for ( String name : sources )
		{
			final PlatynereisDataSource source = mainCommand.dataSourcesMap.get( name );

			if ( source.spimData == null )
			{
				source.spimData = openSpimData( source.file );
			}

			final ViewerImgLoader imgLoader = ( ViewerImgLoader ) source.spimData.getSequenceDescription().getImgLoader();
			final ViewerSetupImgLoader< ?, ? > setupImgLoader = imgLoader.getSetupImgLoader( 0 );

			final AffineTransform3D viewRegistration = source.spimData.getViewRegistrations().getViewRegistration( 0, 0 ).getModel();
			double scale = viewRegistration.get( 0,0 );

			final double[][] resolutions = setupImgLoader.getMipmapResolutions();
			
			int appropriateLevel = getAppropriateLevel( radius, scale, resolutions );

			Utils.getLocalMaximum(
					(RandomAccessibleInterval<T>) setupImgLoader.getVolatileImage( 0, appropriateLevel),
					micrometerMousePosition,
					radius,
					scale * resolutions[ appropriateLevel ][ 0 ] );

		}
	}

	private int getAppropriateLevel( double radius, double scale, double[][] resolutions )
	{
		int appropriateLevel = 0;
		for( int level = 0; level < resolutions.length; ++level )
		{
			double levelBinning = resolutions[ level ][ 0 ];
			if ( levelBinning * scale > radius )
			{
				appropriateLevel = level - 1;
				break;
			}
		}
		return appropriateLevel;
	}

	public void printCoordinates()
	{
		final RealPoint micrometerMousePosition = getMicrometerMousePosition();

		final RealPoint posInverse = new RealPoint( 3 );
		ProSPrRegistration.getTransformationFromEmToProsprInMicrometerUnits().inverse().apply( micrometerMousePosition, posInverse );

		IJ.log( "coordinates in raw em data set [micrometer] : " + Util.printCoordinates( new RealPoint( posInverse ) ) );

	}

	private RealPoint getMicrometerMousePosition()
	{
		final RealPoint posInBdvInMicrometer = new RealPoint( 3 );
		bdv.getBdvHandle().getViewerPanel().getGlobalMouseCoordinates( posInBdvInMicrometer );
		return posInBdvInMicrometer;
	}

	private void addSourceSelectionUI( JPanel panel )
	{
		final JPanel horizontalLayoutPanel = horizontalLayoutPanel();

		horizontalLayoutPanel.add( new JLabel( "Add source to viewer: " ) );

		final JComboBox dataSources = new JComboBox();

		horizontalLayoutPanel.add( dataSources );

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

		panel.add( horizontalLayoutPanel );
	}

	private void addPositionZoomUI( JPanel panel )
	{
		final JPanel horizontalLayoutPanel = horizontalLayoutPanel();

		horizontalLayoutPanel.add( new JLabel( "Move to position [x,y,z]: " ) );

		final JTextField position = new JTextField( "100.0,100.0,100.0" );

		horizontalLayoutPanel.add( position );

		horizontalLayoutPanel.add( new JLabel( "Zoom level: " ) );

		final JTextField zoom = new JTextField( "1.0" );

		horizontalLayoutPanel.add( zoom );
		
		position.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				moveToPosition( Utils.delimitedStringToDoubleArray( position.getText(), ","),
						Double.parseDouble( zoom.getText() )
						);
			}
		} );


		panel.add( horizontalLayoutPanel );
	}

	private void moveToPosition( double[] position, double zoom )
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
