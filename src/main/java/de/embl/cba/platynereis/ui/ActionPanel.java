package de.embl.cba.platynereis.ui;

import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.util.Bdv;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.platynereis.*;
import de.embl.cba.platynereis.utils.Utils;
import de.embl.cba.tables.InteractiveTablePanel;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static de.embl.cba.platynereis.objects.ObjectViewer3D.showSelectedObjectIn3D;
import static de.embl.cba.platynereis.utils.Utils.combine;
import static de.embl.cba.platynereis.utils.Utils.openSpimData;

public class ActionPanel < T extends RealType< T > & NativeType< T > > extends JPanel
{
	public static final int TEXT_FIELD_HEIGHT = 20;
	private final Bdv bdv;
	private final PlatyBrowser platyBrowser;
	private final MainFrame mainFrame;
	private Behaviours behaviours;
	private int geneSearchMipMapLevel;
	private double geneSearchVoxelSize;
	private ArrayList< Double > geneSearchRadii;
	private double[] defaultTargetNormalVector = new double[]{0.70,0.56,0.43};
	private double[] targetNormalVector;
	private InteractiveTablePanel interactiveGeneExpressionTablePanel;

	public ActionPanel( MainFrame mainFrame, Bdv bdv, PlatyBrowser platyBrowser )
	{
		this.mainFrame = mainFrame;
		this.bdv = bdv;
		this.platyBrowser = platyBrowser;

		behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdv.getBdvHandle().getTriggerbindings(), "behaviours" );

		this.setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );

		this.targetNormalVector = Arrays.copyOf(defaultTargetNormalVector, 3);

		addSourceSelectionUI( this );
		addPositionZoomUI( this  );
		addPositionPrintUI( this );
		addSelectionUI( this );
		addLocalGeneSearchUI( this);
		addLeveling( this );

		this.revalidate();
		this.repaint();

	}

	public void addSelectionUI( JPanel panel )
	{
		JPanel horizontalLayoutPanel = horizontalLayoutPanel();

//		horizontalLayoutPanel.add( new JLabel( "[ Shift click ] Select object " ) );
//		horizontalLayoutPanel.add( new JLabel( "[ Double click ] 3D object view " ) );
//		horizontalLayoutPanel.add( new JLabel( "[ Q ] Select none " ) );
//		horizontalLayoutPanel.add( new JLabel( " " ) );

		addObjectSelection( behaviours );

		add3DObjectView( behaviours );

		addSelectNone( behaviours );

		panel.add( horizontalLayoutPanel );

	}

	public ArrayList< Double > getGeneSearchRadii()
	{
		return geneSearchRadii;
	}


	private void addPositionPrintUI( JPanel panel )
	{

		JPanel horizontalLayoutPanel = horizontalLayoutPanel();

		//horizontalLayoutPanel.add( new JLabel( "[ P ] Print current position " ) );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {

			(new Thread(new Runnable(){
				public void run()
				{
					final RealPoint globalMouseCoordinates = BdvUtils.getGlobalMouseCoordinates( bdv );
					Utils.log( "Position: " + globalMouseCoordinates.toString() );
				}
			})).start();

		}, "Print position", "P"  ) ;

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

			(new Thread(new Runnable(){
				public void run()
				{
					showSelectedObjectIn3D( bdv, BdvUtils.getGlobalMouseCoordinates( bdv ), 0.1 );
				}
			})).start();

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

		horizontalLayoutPanel.add( new JLabel( "Gene discovery radius: " ) );

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

			final BdvTextOverlay bdvTextOverlay = new BdvTextOverlay( bdv, "Searching expressed genes; please wait...", micrometerPosition );

			(new Thread(new Runnable(){
				public void run(){
					searchGenes( micrometerPosition, micrometerRadius );
					bdvTextOverlay.setText( "" );
				}
			})).start();


		}, "discover genes", "D" );

		panel.add( horizontalLayoutPanel );

	}

	public void searchGenes( double[] micrometerPosition, double micrometerRadius )
	{
		GeneSearch geneSearch = new GeneSearch(
				micrometerRadius,
				micrometerPosition,
				platyBrowser.dataSources,
				bdv,
				geneSearchMipMapLevel,
				geneSearchVoxelSize );

		final Map< String, Double > geneExpressionLevels = geneSearch.runSearchAndGetLocalExpression();
		final Map< String, Double > sortedGeneExpressionLevels = geneSearch.getSortedExpressionLevels();

		addSortedGenesToViewerPanel( sortedGeneExpressionLevels, 15 );
		addRowToExpressionLevelsTable( micrometerPosition, micrometerRadius, geneExpressionLevels );
		logGeneExpression( micrometerPosition, micrometerRadius, sortedGeneExpressionLevels );

	}

	public void logGeneExpression( double[] micrometerPosition, double micrometerRadius, Map< String, Double > sortedGeneExpressionLevels )
	{
		Utils.log( "\n# Expression levels [fraction of search volume]" );
		Utils.logVector( "Center position [um]" , micrometerPosition );
		Utils.log( "Radius [um]: " + micrometerRadius );
		for ( String gene : sortedGeneExpressionLevels.keySet() )
		{
			Utils.log( gene  + ": " + sortedGeneExpressionLevels.get( gene ) );
		}
	}

	public void addRowToExpressionLevelsTable( double[] micrometerPosition, double micrometerRadius, Map< String, Double > geneExpressionLevels )
	{
		if ( interactiveGeneExpressionTablePanel == null )
		{
			initGeneExpressionTable( geneExpressionLevels );
		}

		final Double[] position = { micrometerPosition [ 0 ], micrometerPosition[ 1 ], micrometerPosition[ 2 ], 0.0 };
		final Double[] parameters = { micrometerRadius };
		final Double[] expressionLevels = geneExpressionLevels.values().toArray( new Double[ geneExpressionLevels.size() ] );
		interactiveGeneExpressionTablePanel.addRow( combine( combine( position, parameters ), expressionLevels ) );
	}

	public void initGeneExpressionTable( Map< String, Double > geneExpressionLevels )
	{
		final String[] position = { "X", "Y", "Z", "T" };
		final String[] searchParameters = { "SearchRadius_um" };
		final String[] genes = geneExpressionLevels.keySet().toArray( new String[ geneExpressionLevels.keySet().size() ] );

		interactiveGeneExpressionTablePanel = new InteractiveTablePanel( combine( combine( position, searchParameters ), genes ) );
		interactiveGeneExpressionTablePanel.setCoordinateColumns( new int[]{ 0, 1, 2, 3 }  );
		interactiveGeneExpressionTablePanel.setBdv( bdv );
	}

	public void addSortedGenesToViewerPanel( Map sortedExpressionLevels, int maxNumGenes )
	{
		final ArrayList< String > sortedGenes = new ArrayList( sortedExpressionLevels.keySet() );

		if ( sortedGenes.size() > 0 )
		{
			mainFrame.getBdvSourcesPanel().removeAllProSPrSources();

			for ( int i = sortedGenes.size()-1; i > sortedGenes.size()- maxNumGenes && i >= 0; --i )
			{
				mainFrame.getBdvSourcesPanel().addSourceToViewerAndPanel( sortedGenes.get( i ) );
			}
		}
	}

	private void setGeneSearchRadii( )
	{
		final Set< String > sources = platyBrowser.dataSources.keySet();

		geneSearchRadii = new ArrayList<>();

		for ( String name : sources )
		{
			if ( name.contains( Constants.EM_FILE_ID ) ) continue;

			final PlatynereisDataSource source = platyBrowser.dataSources.get( name );

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

	private void addSourceSelectionUI( JPanel panel )
	{
		final JPanel horizontalLayoutPanel = horizontalLayoutPanel();

		horizontalLayoutPanel.add( new JLabel( "Add to viewer: " ) );

		final JComboBox dataSources = new JComboBox();

		horizontalLayoutPanel.add( dataSources );

		for ( String name : platyBrowser.dataSources.keySet() )
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
		position.setMaximumSize( new Dimension( 10, TEXT_FIELD_HEIGHT ) );

		horizontalLayoutPanel.add( position );

		horizontalLayoutPanel.add( new JLabel( "  Zoom factor: " ) );

		final JTextField zoom = new JTextField( " 15 " );
		zoom.setMaximumSize( new Dimension( 10, TEXT_FIELD_HEIGHT ) );

		horizontalLayoutPanel.add( zoom );
		
		position.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				BdvUtils.zoomToPosition(
						bdv,
						Utils.delimitedStringToDoubleArray( position.getText(), ","),
						Double.parseDouble( zoom.getText() ), 1000 );
			}
		} );

		zoom.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				BdvUtils.zoomToPosition(
						bdv,
						Utils.delimitedStringToDoubleArray( position.getText(), ","),
						Double.parseDouble( zoom.getText() ),
						1000 );
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
