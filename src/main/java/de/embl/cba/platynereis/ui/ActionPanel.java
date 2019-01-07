package de.embl.cba.platynereis.ui;

import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.util.Bdv;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.platynereis.Constants;
import de.embl.cba.platynereis.GeneSearch;
import de.embl.cba.platynereis.PlatyBrowser;
import de.embl.cba.platynereis.PlatySource;
import de.embl.cba.platynereis.utils.Utils;
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

import static de.embl.cba.platynereis.utils.Utils.openSpimData;

public class ActionPanel < T extends RealType< T > & NativeType< T > > extends JPanel
{
	public static final int TEXT_FIELD_HEIGHT = 20;
	private final Bdv bdv;
	private final PlatyBrowser platyBrowser;
	private final MainUI mainUI;
	private Behaviours behaviours;
	private int geneSearchMipMapLevel;
	private double geneSearchVoxelSize;
	private ArrayList< Double > geneSearchRadii;

	private double[] defaultTargetNormalVector = new double[]{0.70,0.56,0.43};
	private double[] targetNormalVector;

	public ActionPanel( MainUI mainUI, Bdv bdv, PlatyBrowser platyBrowser )
	{
		this.mainUI = mainUI;
		this.bdv = bdv;
		this.platyBrowser = platyBrowser;

		behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdv.getBdvHandle().getTriggerbindings(), "behaviours" );

		this.setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );

		this.targetNormalVector = Arrays.copyOf(defaultTargetNormalVector, 3);

		addSourceSelectionUI( this );
		addPositionZoomUI( this  );
		addPositionPrintUI( this );
		addLocalGeneSearchRadiusUI( this);
		add3DObjectViewResolutionUI( this );
		addLeveling( this );

		this.revalidate();
		this.repaint();

	}

	public ArrayList< Double > getGeneSearchRadii()
	{
		return geneSearchRadii;
	}

	private void addPositionPrintUI( JPanel panel )
	{

		JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

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

	private void add3DObjectViewResolutionUI( JPanel panel )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		horizontalLayoutPanel.add( new JLabel( "3D object view resolution [micrometer]: " ) );

		final JComboBox resolutionComboBox = createResolutionComboBox();

		resolutionComboBox.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				final ArrayList< PlatySource > activePlatySources = platyBrowser.getPlatySources( mainUI.getBdvSourcesPanel().getCurrentSourceNames() );

				for ( PlatySource source : activePlatySources )
				{
					if ( source.bdvSelectionEventHandler != null )
					{
						source.bdvSelectionEventHandler.set3DObjectViewResolution(
								 (double) resolutionComboBox.getSelectedItem() );
					}
				}
			}
		} );

		horizontalLayoutPanel.add( resolutionComboBox );

		panel.add( horizontalLayoutPanel );
	}

	private JComboBox createResolutionComboBox()
	{
		final JComboBox resolutionComboBox = new JComboBox( );

		final ArrayList< Double > resolutions = new ArrayList<>();
//		resolutions.add( 2.0 );
//		resolutions.add( 1.0 );
//		resolutions.add( 0.5 );
		resolutions.add( 0.25 );
		resolutions.add( 0.10 );
		resolutions.add( 0.05 );
		resolutions.add( 0.01 );

		for ( double resolution : resolutions )
		{
			resolutionComboBox.addItem( resolution );
		}

		resolutionComboBox.setSelectedIndex( 0 );

		return resolutionComboBox;
	}

	private void addLocalGeneSearchRadiusUI( JPanel panel )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		horizontalLayoutPanel.add( new JLabel( "Gene discovery radius [micrometer]: " ) );

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

		GeneExpressions.addRowToGeneExpressionTable( micrometerPosition, micrometerRadius, geneExpressionLevels );
		GeneExpressions.logGeneExpression( micrometerPosition, micrometerRadius, sortedGeneExpressionLevels );

	}

	public void addSortedGenesToViewerPanel( Map sortedExpressionLevels, int maxNumGenes )
	{
		final ArrayList< String > sortedGenes = new ArrayList( sortedExpressionLevels.keySet() );

		if ( sortedGenes.size() > 0 )
		{
			mainUI.getBdvSourcesPanel().removeAllProSPrSources();

			for ( int i = sortedGenes.size()-1; i > sortedGenes.size()- maxNumGenes && i >= 0; --i )
			{
				mainUI.getBdvSourcesPanel().addSourceToViewerAndPanel( sortedGenes.get( i ) );
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

			final PlatySource source = platyBrowser.dataSources.get( name );

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
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

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
				mainUI.getBdvSourcesPanel().addSourceToViewerAndPanel( (String) dataSources.getSelectedItem() );
			}
		} );

		panel.add( horizontalLayoutPanel );
	}

	private void addLeveling( JPanel panel )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton levelCurrentView = new JButton( "Level current view" );
		horizontalLayoutPanel.add( levelCurrentView );

		final JButton changeReference = new JButton( "Set new level vector" );
		horizontalLayoutPanel.add( changeReference );

		final JButton defaultReference = new JButton( "Set default level vector" );
		horizontalLayoutPanel.add( defaultReference );


		levelCurrentView.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				BdvUtils.levelCurrentView( bdv, targetNormalVector );
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
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

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


}
