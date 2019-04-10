package de.embl.cba.platynereis.platybrowser;

import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.platynereis.Constants;
import de.embl.cba.platynereis.GeneSearch;
import de.embl.cba.platynereis.GeneSearchResults;
import de.embl.cba.platynereis.utils.SortIgnoreCase;
import de.embl.cba.platynereis.utils.Utils;
import de.embl.cba.platynereis.utils.ui.BdvTextOverlay;
import de.embl.cba.tables.SwingUtils;
import de.embl.cba.tables.modelview.images.ImageSourcesModel;
import de.embl.cba.tables.modelview.images.SourceAndMetadata;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RealPoint;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class PlatyBrowserActionPanel< T extends RealType< T > & NativeType< T > > extends JPanel
{
	public static final int TEXT_FIELD_HEIGHT = 20;

	private final PlatyBrowserSourcesPanel sourcesPanel;
	private BdvHandle bdv;
	private Behaviours behaviours;
	private double geneSearchVoxelSize;
	private java.util.List< Double > geneSearchRadii;

	private double[] defaultTargetNormalVector = new double[]{0.70,0.56,0.43};
	private double[] targetNormalVector;
	private ImageSourcesModel imageSourcesModel;

	public PlatyBrowserActionPanel( PlatyBrowserSourcesPanel sourcesPanel )
	{
		this.sourcesPanel = sourcesPanel;
		configBdv( sourcesPanel );
		addSourceSelectionUI( this );
		addPositionZoomUI( this  );
		addPositionPrintBehaviour( this );
		addLocalGeneSearchBehaviourAndUI( this);
		//add3DObjectViewResolutionUI( this );
		addLevelingUI( this );
		configPanel();
	}

	public void configBdv( PlatyBrowserSourcesPanel sourcesPanel )
	{
		bdv = sourcesPanel.getBdv();
		behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( this.bdv.getBdvHandle().getTriggerbindings(), "behaviours" );
	}

	public void configPanel()
	{
		this.setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
		this.revalidate();
		this.repaint();
	}

	public java.util.List< Double > getGeneSearchRadii()
	{
		return geneSearchRadii;
	}

	private void addPositionPrintBehaviour( JPanel panel )
	{
		JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {

			(new Thread( () -> {
				final RealPoint globalMouseCoordinates = BdvUtils.getGlobalMouseCoordinates( bdv );
				Utils.log( "Position: " + globalMouseCoordinates.toString() );
			} )).start();

		}, "Print position", "P"  ) ;

		panel.add( horizontalLayoutPanel );
	}

	private void add3DObjectViewResolutionUI( JPanel panel )
	{
		// TODO
//		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();
//
//		horizontalLayoutPanel.add( new JLabel( "3D object view resolution [micrometer]: " ) );
//
//		final JComboBox< Double > resolutionComboBox = createResolutionComboBox();
//
//		resolutionComboBox.addActionListener( new ActionListener()
//		{
//			@Override
//			public void actionPerformed( ActionEvent e )
//			{
//				bdvView.setVoxelSpacing3DView( ( Double ) resolutionComboBox.getSelectedItem() );
//			}
//		} );
//
//		horizontalLayoutPanel.add( resolutionComboBox );
//
//		panel.add( horizontalLayoutPanel );
	}

	private JComboBox< Double > createResolutionComboBox()
	{
		final JComboBox< Double > resolutionComboBox = new JComboBox( );

		final ArrayList< Double > resolutions = new ArrayList<>();
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

	private void addLocalGeneSearchBehaviourAndUI( JPanel panel )
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
				public void run()
				{
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
				imageSourcesModel,
				geneSearchVoxelSize );

		final Map< String, Double > geneExpressionLevels =
				geneSearch.runSearchAndGetLocalExpression();

		final Map< String, Double > sortedGeneExpressionLevels =
				geneSearch.getSortedExpressionLevels();

		addSortedGenesToViewerPanel( sortedGeneExpressionLevels, 15 );

		GeneSearchResults.addRowToGeneExpressionTable(
				micrometerPosition, micrometerRadius, geneExpressionLevels );

		GeneSearchResults.logGeneExpression(
				micrometerPosition, micrometerRadius, sortedGeneExpressionLevels );

	}

	public void addSortedGenesToViewerPanel( Map sortedExpressionLevels, int maxNumGenes )
	{
		final ArrayList< String > sortedGenes = new ArrayList( sortedExpressionLevels.keySet() );

		if ( sortedGenes.size() > 0 )
		{
			// TODO
//			mainUI.getBdvSourcesPanel().removeAllProSPrSources();
//
//			for ( int i = sortedGenes.size()-1; i > sortedGenes.size()- maxNumGenes && i >= 0; --i )
//			{
//				mainUI.getBdvSourcesPanel().addSourceToViewerAndPanel( sortedGenes.get( i ) );
//			}
		}
	}

	private void setGeneSearchRadii( )
	{
		final Set< String > sourceNames = imageSourcesModel.sources().keySet();

		geneSearchRadii = new ArrayList<>();

		for ( String sourceName : sourceNames )
		{
			if ( sourceName.contains( Constants.EM_FILE_ID ) ) continue;

			final SourceAndMetadata sourceAndMetadata = imageSourcesModel.sources().get( sourceName );

			final VoxelDimensions voxelDimensions = sourceAndMetadata.source().getVoxelDimensions();

			geneSearchVoxelSize = voxelDimensions.dimension( 0 );

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

		// ComboBox
		final JComboBox sourcesComboBox = new JComboBox();
		final ArrayList< String > sourceNames = getSortedSourceNames();
		for ( String sourceName : sourceNames )
			sourcesComboBox.addItem( sourceName );

		// Button
		final JButton addToViewer = new JButton( "Add to viewer" );
		addToViewer.addActionListener( e ->
				sourcesPanel.addSourceToPanelAndViewer(
						( String ) sourcesComboBox.getSelectedItem() ) );

		horizontalLayoutPanel.add( addToViewer );
		horizontalLayoutPanel.add( sourcesComboBox );
		panel.add( horizontalLayoutPanel );
	}

	private ArrayList< String > getSortedSourceNames()
	{
		final ArrayList< String > sourceNames = new ArrayList<>( sourcesPanel.getSourceNames() );
		Collections.sort( sourceNames, new SortIgnoreCase() );
		return sourceNames;
	}

	private void addLevelingUI( JPanel panel )
	{
		this.targetNormalVector = Arrays.copyOf( defaultTargetNormalVector, 3 );

		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton levelCurrentView = new JButton( "Level current view" );
		horizontalLayoutPanel.add( levelCurrentView );

		final JButton changeReference = new JButton( "Set new level vector" );
		horizontalLayoutPanel.add( changeReference );

		final JButton defaultReference = new JButton( "Set default level vector" );
		horizontalLayoutPanel.add( defaultReference );

		changeReference.addActionListener( e -> {
			targetNormalVector = BdvUtils.getCurrentViewNormalVector( bdv );
			Utils.logVector( "New reference normal vector: ", targetNormalVector );
		} );

		defaultReference.addActionListener( e -> {
			targetNormalVector = Arrays.copyOf( defaultTargetNormalVector, 3);
			Utils.logVector( "New reference normal vector (default): ", defaultTargetNormalVector );
		} );

		levelCurrentView.addActionListener( e -> BdvUtils.levelCurrentView( bdv, targetNormalVector ) );

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
		
		position.addActionListener( e -> BdvUtils.zoomToPosition(
				bdv,
				Utils.delimitedStringToDoubleArray( position.getText(), ","),
				Double.parseDouble( zoom.getText() ), 1000 ) );

		zoom.addActionListener( e -> BdvUtils.zoomToPosition(
				bdv,
				Utils.delimitedStringToDoubleArray( position.getText(), ","),
				Double.parseDouble( zoom.getText() ),
				1000 ) );


		panel.add( horizontalLayoutPanel );
	}


}
