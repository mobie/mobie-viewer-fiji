package de.embl.cba.platynereis.ui;

import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.util.Bdv;
import de.embl.cba.platynereis.*;
import ij.IJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static de.embl.cba.platynereis.Utils.openSpimData;

public class ActionPanel< T extends RealType< T > & NativeType< T > > extends JPanel
{
	final Bdv bdv;
	JFrame frame;
	private double zoom;
	MainCommand mainCommand;
	private Behaviours behaviours;
	private int geneSearchMipMapLevel;
	private double geneSearchVoxelSize;
	private ArrayList< Double > geneSearchRadii;

	public ActionPanel( Bdv bdv, MainCommand mainCommand )
	{
		this.bdv = bdv;
		this.mainCommand = mainCommand;
		zoom = 10.0;
		behaviours = new Behaviours( new InputTriggerConfig() );

		this.setLayout( new GridLayout() );

		addSourceSelectionUI( this );
		addPositionZoomUI( this );
		addPositionPrintUI();
		addLocalGeneSearchUI();
	}

	public JPanel getPanel()
	{
		return this;
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

	private void addLocalGeneSearchUI()
	{
		final JPanel panel = horizontalLayoutPanel();

		panel.add( new JLabel( "[X] Search genes within radius: " ) );

		setGeneSearchRadii();

		final JComboBox radiiComboBox = new JComboBox( );
		for ( double radius : geneSearchRadii )
		{
			radiiComboBox.addItem( "" + radius );
		}

		panel.add( radiiComboBox );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			searchNearbyGenes( Double.parseDouble(  (String) radiiComboBox.getSelectedItem() ) );
		}, "show genes", "X" );

		add( panel );

	}


	private void setGeneSearchRadii( )
	{
		final Set< String > sources = mainCommand.dataSourcesMap.keySet();

		geneSearchRadii = new ArrayList<>();

		for ( String name : sources )
		{
			if ( name.contains( Constants.EM_FILE_ID ) ) continue;

			final PlatynereisDataSource source = mainCommand.dataSourcesMap.get( name );

			if ( source.spimData == null )
			{
				source.spimData = openSpimData( source.file );
			}

			final ViewerImgLoader imgLoader = ( ViewerImgLoader ) source.spimData.getSequenceDescription().getImgLoader();
			final ViewerSetupImgLoader< ?, ? > setupImgLoader = imgLoader.getSetupImgLoader( 0 );
			final AffineTransform3D viewRegistration = source.spimData.getViewRegistrations().getViewRegistration( 0, 0 ).getModel();

			double scale = viewRegistration.get( 0, 0 );
			final double[][] resolutions = setupImgLoader.getMipmapResolutions();

			geneSearchMipMapLevel = resolutions.length - 1;
			geneSearchVoxelSize = scale * resolutions[ geneSearchMipMapLevel ][ 0 ];
			geneSearchRadii.add( 2 * geneSearchVoxelSize );

			break;
		}

		geneSearchRadii.add( 4 * geneSearchVoxelSize );
		geneSearchRadii.add( 8 * geneSearchVoxelSize );
	}



	private void searchNearbyGenes( double micrometerRadius )
	{
		Utils.log( "Searching genes..." );

		double[] micrometerMousePosition = new double[ 3 ];
		getMicrometerMousePosition().localize( micrometerMousePosition  );

		final Set< String > sources = mainCommand.dataSourcesMap.keySet();
		Map< String, Double > localMaxima = new LinkedHashMap<>(  );

		int n = sources.size();
		int i = 1;
		for ( String name : sources )
		{

			if ( name.contains( Constants.EM_FILE_ID ) ) continue;

			final PlatynereisDataSource source = mainCommand.dataSourcesMap.get( name );

			if ( source.spimData == null )
			{
				source.spimData = openSpimData( source.file );
			}

			final ViewerImgLoader imgLoader = ( ViewerImgLoader ) source.spimData.getSequenceDescription().getImgLoader();
			final ViewerSetupImgLoader< ?, ? > setupImgLoader = imgLoader.getSetupImgLoader( 0 );
			final RandomAccessibleInterval< T > image = (RandomAccessibleInterval<T>) setupImgLoader.getImage( 0, 2 );

			final double localMaximum = Utils.getLocalMaximum(
					image,
					micrometerMousePosition,
					micrometerRadius,
					geneSearchVoxelSize,
					name );

			localMaxima.put( name, localMaximum );

			//Utils.log( "" + i++ + "/" + n + ";" + name + ": " + localMaximum );

		}

		final Map< String, Double > sortedMaxima = Utils.sortByValue( localMaxima );
		final ArrayList sortedNames = new ArrayList( sortedMaxima.keySet() );

		Utils.log( "## Nearby gene list " );
		for ( i = 0; i < sortedMaxima.size(); ++i )
		{
			String name = ( String ) sortedNames.get( i );
			Utils.log( name + ": " + sortedMaxima.get( name ) );
		}

//		mainCommand.addSourceToBdv(  );
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

		horizontalLayoutPanel.add( new JLabel( "Add to viewer: " ) );

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
				mainCommand.addSourceToBdv( (String) dataSources.getSelectedItem() );
			}
		} );

		panel.add( horizontalLayoutPanel );
	}

	private void addPositionZoomUI( JPanel panel )
	{
		final JPanel horizontalLayoutPanel = horizontalLayoutPanel();

		horizontalLayoutPanel.add( new JLabel( "Move to [x,y,z]: " ) );

		final JTextField position = new JTextField( "100.0,100.0,100.0" );

		horizontalLayoutPanel.add( position );

		horizontalLayoutPanel.add( new JLabel( "  Zoom level: " ) );

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
		panel.setLayout( new BoxLayout( panel, BoxLayout.LINE_AXIS ) );
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
