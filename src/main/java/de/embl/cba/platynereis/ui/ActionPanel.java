package de.embl.cba.platynereis.ui;

import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.util.Bdv;
import bdv.util.BdvSource;
import bdv.util.BdvStackSource;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.platynereis.*;
import ij.IJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
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
import java.util.Set;

import static de.embl.cba.platynereis.Utils.openSpimData;

public class ActionPanel < T extends RealType< T > & NativeType< T > > extends JPanel
{
	private final Bdv bdv;
	private final MainCommand mainCommand;
	private final MainFrame mainFrame;
	private Behaviours behaviours;
	private int geneSearchMipMapLevel;
	private double geneSearchVoxelSize;
	private ArrayList< Double > geneSearchRadii;

	public ActionPanel( MainFrame mainFrame, Bdv bdv, MainCommand mainCommand )
	{
		this.mainFrame = mainFrame;
		this.bdv = bdv;
		this.mainCommand = mainCommand;
		behaviours = new Behaviours( new InputTriggerConfig() );
		this.setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );

		addSourceSelectionUI( this );
		addPositionZoomUI( this  );
		addSelectionUI( this );
		addLocalGeneSearchUI( this);
		addLeveling( this );

		this.revalidate();
		this.repaint();

	}

	public void addSelectionUI( JPanel panel )
	{
		JPanel horizontalLayoutPanel = horizontalLayoutPanel();

		behaviours.install( bdv.getBdvHandle().getTriggerbindings(), "behaviours" );


		horizontalLayoutPanel.add( new JLabel( "[ P ] Select " ) );
		horizontalLayoutPanel.add( new JLabel( " " ) );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			printCoordinates();
		}, "print pos", "P" );

		panel.add( horizontalLayoutPanel );

	}

	private void addLocalGeneSearchUI( JPanel panel )
	{
		final JPanel horizontalLayoutPanel = horizontalLayoutPanel();

		horizontalLayoutPanel.add( new JLabel( "[ D ] Discover genes within radius: " ) );

		setGeneSearchRadii();

		final JComboBox radiiComboBox = new JComboBox( );
		for ( double radius : geneSearchRadii )
		{
			radiiComboBox.addItem( "" + radius );
		}

		horizontalLayoutPanel.add( radiiComboBox );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {

			double[] micrometerPosition = new double[ 3 ];
			getMicrometerMousePosition().localize( micrometerPosition );

			double micrometerRadius = Double.parseDouble( ( String ) radiiComboBox.getSelectedItem() );

			(new Thread(new Runnable(){
				public void run(){

					searchNearbyGenes( micrometerPosition, micrometerRadius );
				}
			})).start();


		}, "search genes", "D" );

		panel.add( horizontalLayoutPanel );

	}

	private void searchNearbyGenes( double[] micrometerPosition, double micrometerRadius )
	{

		GeneSearch geneSearch = new GeneSearch(
				micrometerRadius,
				micrometerPosition,
				mainCommand.dataSources,
				bdv,
				geneSearchMipMapLevel,
				geneSearchVoxelSize );

		geneSearch.run();

		// TODO: this seems overly complicated, put the thread logic into gene search itself?!
		while( ! geneSearch.isDone() )
		{
			wait100ms();
		}

		final ArrayList< String > genes = new ArrayList( geneSearch.getSortedGenes().keySet() );

		if ( genes.size() > 0 )
		{
			mainFrame.getBdvSourcesPanel().removeAllProSPrSources();

			for ( int i = genes.size() - 1; i > genes.size() - 10; --i )
			{
				mainFrame.getBdvSourcesPanel().addSourceToViewerAndPanel( genes.get( i ) );

//				if ( i == genes.size() - 1 )
//				{
//					mainFrame.getBdvSourcesPanel().toggleVisibility( genes.get( i ) );
//				}
			}
		}
	}

	private void wait100ms()
	{
		try
		{
			Thread.sleep( 100 );
		}
		catch ( InterruptedException e )
		{
			e.printStackTrace();
		}
	}


	private void setGeneSearchRadii( )
	{
		final Set< String > sources = mainCommand.dataSources.keySet();

		geneSearchRadii = new ArrayList<>();

		for ( String name : sources )
		{
			if ( name.contains( Constants.EM_FILE_ID ) ) continue;

			final PlatynereisDataSource source = mainCommand.dataSources.get( name );

			if ( source.spimData == null )
			{
				source.spimData = openSpimData( source.file );
			}

			final ViewerImgLoader imgLoader = ( ViewerImgLoader ) source.spimData.getSequenceDescription().getImgLoader();
			final ViewerSetupImgLoader< ?, ? > setupImgLoader = imgLoader.getSetupImgLoader( 0 );
			final AffineTransform3D viewRegistration = source.spimData.getViewRegistrations().getViewRegistration( 0, 0 ).getModel();

			double scale = viewRegistration.get( 0, 0 );
			final double[][] resolutions = setupImgLoader.getMipmapResolutions();

			geneSearchMipMapLevel = 0; // highest resolution
			geneSearchVoxelSize = scale * resolutions[ geneSearchMipMapLevel ][ 0 ];

			break;
		}

		for ( int i = 0; i < 8; ++i )
		{
			geneSearchRadii.add( Math.pow( 2, i ) * geneSearchVoxelSize );
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

		//final RealPoint posInverse = new RealPoint( 3 );
		//ProSPrRegistration.getTransformationFromEmToProsprInMicrometerUnits().inverse().apply( micrometerMousePosition, posInverse );
		//IJ.log( "coordinates in raw em data set [micrometer] : " + Util.printCoordinates( new RealPoint( posInverse ) ) );

		IJ.log( "coordinates in raw em data set [micrometer] : " + Util.printCoordinates( micrometerMousePosition ) );

	}


	public void selectObject()
	{
		final RealPoint micrometerMousePosition = getMicrometerMousePosition();

		final ArrayList< String > currentSourceNames = mainFrame.getBdvSourcesPanel().getCurrentSourceNames();

		for ( String name : currentSourceNames )
		{
			final BdvStackSource bdvStackSource = mainCommand.dataSources.get( name ).bdvStackSource;

			if ( bdvStackSource == null || ! BdvUtils.isLabelsSource( bdvStackSource ) )
			{
				continue;
			}

			final RandomAccessibleInterval< IntegerType > indexImg = BdvUtils.getIndexImg( bdvStackSource, 0, 0 );

			final AffineTransform3D sourceTransform = BdvUtils.getSourceTransform( bdvStackSource, 0, 0, 0 );

			// sourceTransform.apply(  );
		}
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

		for ( String name : mainCommand.dataSources.keySet() )
		{
			dataSources.addItem( name );
		}

		dataSources.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				mainFrame.getBdvSourcesPanel().addSourceToViewerAndPanel( (String) dataSources.getSelectedItem() );
			}
		} );

		panel.add( horizontalLayoutPanel );
	}

	private void addLeveling( JPanel panel )
	{
		final JPanel horizontalLayoutPanel = horizontalLayoutPanel();

		final JButton level = new JButton( "Level current view" );
		horizontalLayoutPanel.add( level );

		horizontalLayoutPanel.add( new JLabel( "Target normal vector: " ) );

		final JTextField normalVectorTextField = new JTextField( "0.7041,0.5650,0.43007" );
		horizontalLayoutPanel.add( normalVectorTextField );

		level.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				double[] normalVector = Utils.getCSVasDoubles( normalVectorTextField.getText() );
				Utils.level( bdv, normalVector );
			}
		} );

		panel.add( horizontalLayoutPanel );
	}

	private void addPositionZoomUI( JPanel panel )
	{
		final JPanel horizontalLayoutPanel = horizontalLayoutPanel();

		horizontalLayoutPanel.add( new JLabel( "Move to [x,y,z]: " ) );

		final JTextField position = new JTextField( "  177, 218,  67  " );

		horizontalLayoutPanel.add( position );

		horizontalLayoutPanel.add( new JLabel( "  Zoom factor: " ) );

		final JTextField zoom = new JTextField( " 15 " );

		horizontalLayoutPanel.add( zoom );
		
		position.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				Utils.centerBdvViewToPosition(
						Utils.delimitedStringToDoubleArray( position.getText(), ","),
						Double.parseDouble( zoom.getText() ),
						bdv );
			}
		} );

		zoom.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				Utils.centerBdvViewToPosition(
						Utils.delimitedStringToDoubleArray( position.getText(), ","),
						Double.parseDouble( zoom.getText() ),
						bdv );
			}
		} );


		panel.add( horizontalLayoutPanel );
	}


	private JPanel horizontalLayoutPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.LINE_AXIS ) );
		panel.setBorder( BorderFactory.createEmptyBorder(0, 10, 10, 10) );
		panel.add( Box.createHorizontalGlue() );
		panel.setAlignmentX( Component.LEFT_ALIGNMENT );

		return panel;
	}


}
