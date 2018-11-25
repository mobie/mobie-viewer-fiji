package de.embl.cba.platynereis.ui;

import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.util.Bdv;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.platynereis.*;
import ij.IJ;
import ij.ImagePlus;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.vecmath.Color3f;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
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
	private double[] defaultTargetNormalVector = new double[]{0.70,0.56,0.43};
	private double[] targetNormalVector;

	public ActionPanel( MainFrame mainFrame, Bdv bdv, MainCommand mainCommand )
	{
		this.mainFrame = mainFrame;
		this.bdv = bdv;
		this.mainCommand = mainCommand;

		behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdv.getBdvHandle().getTriggerbindings(), "behaviours" );

		this.setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );

		this.targetNormalVector = Arrays.copyOf(defaultTargetNormalVector, 3);

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

		horizontalLayoutPanel.add( new JLabel( "[ Shift click ] Select object" ) );
		horizontalLayoutPanel.add( new JLabel( "[ Double click ] 3D object view" ) );
		horizontalLayoutPanel.add( new JLabel( "[ Q ] Select none " ) );
		horizontalLayoutPanel.add( new JLabel( " " ) );

		addObjectSelection( behaviours );

		add3DObjectView( behaviours );

		addSelectNone( behaviours );

		panel.add( horizontalLayoutPanel );

	}

	private void addSelectNone( Behaviours behaviours )
	{
		behaviours.install( bdv.getBdvHandle().getTriggerbindings(), "behaviours" );
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			BdvUtils.deselectAllObjectsInActiveLabelSources( bdv );
		}, "select none", "Q" );
	}

	private void add3DObjectView( Behaviours behaviours )
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {

			final RealPoint globalMouseCoordinates = BdvUtils.getGlobalMouseCoordinates( bdv );

			// TODO: Also get calibrations back
			final ArrayList< RandomAccessibleInterval< BitType > > masks = BdvUtils.extractSelectedObject( bdv, globalMouseCoordinates, 3 );

			final ImagePlus mask = Utils.asImagePlus( masks.get( 0 ) );

			final ImagePlus duplicate = mask.duplicate();

			duplicate.show();

//			Image3DUniverse univ = new Image3DUniverse( );
//			univ.show( );
//			final Content content = univ.addMesh( duplicate, null, "object", 250, new boolean[]{ true, true, true }, 2 );
//			content.setColor( new Color3f(1.0f, 1.0f, 1.0f ) );


		}, "3d object view", "button1 double-click"  ) ;
	}

	private void addObjectSelection( Behaviours behaviours )
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {

			final RealPoint globalMouseCoordinates = BdvUtils.getGlobalMouseCoordinates( bdv );

			final Map< Integer, Long > integerLongMap = BdvUtils.selectObjectsInActiveLabelSources( bdv, globalMouseCoordinates );

			for ( int sourceIndex : integerLongMap.keySet())
			{
				Utils.log( "Label " + integerLongMap.get( sourceIndex ) + " selected in source #" + sourceIndex );
			};

		}, "select object", "shift button1"  ) ;
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
			BdvUtils.getGlobalMouseCoordinates( bdv ).localize( micrometerPosition );

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

		//final RealPoint posInverse = new RealPoint( 3 );
		//ProSPrRegistration.getTransformationFromEmToProsprInMicrometerUnits().inverse().apply( micrometerMousePosition, posInverse );
		//IJ.log( "coordinates in raw em data set [micrometer] : " + Util.printCoordinates( new RealPoint( posInverse ) ) );

		IJ.log( "coordinates in raw em data set [micrometer] : " + Util.printCoordinates( BdvUtils.getGlobalMouseCoordinates( bdv ) ) );

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

		final JButton levelCurrentView = new JButton( "Level" );
		horizontalLayoutPanel.add( levelCurrentView );

		final JButton changeReference = new JButton( "Set new" );
		horizontalLayoutPanel.add( changeReference );

		final JButton defaultReference = new JButton( "Set default" );
		horizontalLayoutPanel.add( defaultReference );


		levelCurrentView.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				BdvUtils.levelView( bdv, targetNormalVector );
			}
		} );

		changeReference.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				targetNormalVector = BdvUtils.getCurrentViewNormalVector( bdv );
				Utils.logVector( "New reference normal vector: ", targetNormalVector );
			}
		} );


		defaultReference.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				targetNormalVector =  Arrays.copyOf(defaultTargetNormalVector, 3);
				Utils.logVector( "New reference normal vector (default): ", defaultTargetNormalVector );

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
				BdvUtils.centerBdvViewToPosition(
						bdv,
						Utils.delimitedStringToDoubleArray( position.getText(), ","),
						Double.parseDouble( zoom.getText() ) );
			}
		} );

		zoom.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				BdvUtils.centerBdvViewToPosition(
						bdv,
						Utils.delimitedStringToDoubleArray( position.getText(), ","),
						Double.parseDouble( zoom.getText() ) );
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
