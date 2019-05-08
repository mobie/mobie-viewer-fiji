package de.embl.cba.platynereis.platybrowser;

import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.platynereis.Constants;
import de.embl.cba.platynereis.GeneSearch;
import de.embl.cba.platynereis.GeneSearchResults;
import de.embl.cba.platynereis.Globals;
import de.embl.cba.platynereis.utils.BdvViewChanger;
import de.embl.cba.platynereis.utils.SortIgnoreCase;
import de.embl.cba.platynereis.utils.Utils;
import de.embl.cba.platynereis.utils.ui.BdvTextOverlay;
import de.embl.cba.tables.SwingUtils;
import de.embl.cba.tables.image.SourceAndMetadata;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public class PlatyBrowserActionPanel extends JPanel
{
	public static final int TEXT_FIELD_HEIGHT = 20;

	private final PlatyBrowserSourcesPanel sourcesPanel;
	private BdvHandle bdv;
	private Behaviours behaviours;

	private double[] defaultTargetNormalVector = new double[]{0.70,0.56,0.43};
	private double[] targetNormalVector;
	private double geneSearchRadiusInMicrometer;

	public PlatyBrowserActionPanel( PlatyBrowserSourcesPanel sourcesPanel )
	{
		this.sourcesPanel = sourcesPanel;
		bdv = sourcesPanel.getBdv();
		installBdvBehaviours();

		addSourceSelectionUI( this );
		addMoveToViewUI( this  );
		addLevelingUI( this );
		addShowSegmentsIn3DUI( this );
		configPanel();
	}

	private void addShowSegmentsIn3DUI( JPanel panel )
	{
		JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JCheckBox checkBox = new JCheckBox( "Show selected objects in 3D" );
		checkBox.setSelected( Globals.showSegmentsIn3D.get() );
		checkBox.addActionListener( e -> {
			Globals.showSegmentsIn3D.set( checkBox.isSelected() );
		} );

		horizontalLayoutPanel.add( checkBox );
		panel.add( horizontalLayoutPanel );
	}

	public double getGeneSearchRadiusInMicrometer()
	{
		return geneSearchRadiusInMicrometer;
	}

	public void setGeneSearchRadiusInMicrometer( double geneSearchRadiusInMicrometer )
	{
		this.geneSearchRadiusInMicrometer = geneSearchRadiusInMicrometer;
	}

	private void installBdvBehaviours()
	{
		behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdv.getTriggerbindings(), "behaviours" );
		addPositionAndViewLoggingBehaviour( this );
		addLocalGeneSearchBehaviour();
	}

	private void configPanel()
	{
		this.setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
		this.revalidate();
		this.repaint();
	}

	private void addPositionAndViewLoggingBehaviour( JPanel panel )
	{
		JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {

			(new Thread( () -> {
				logPositionAndView();
			} )).start();

		}, "Print position and view", "P"  ) ;

		panel.add( horizontalLayoutPanel );
	}

	private void logPositionAndView()
	{
		final RealPoint globalMouseCoordinates = BdvUtils.getGlobalMouseCoordinates( bdv );
		Utils.log( "Position: " + globalMouseCoordinates.toString() );
		final AffineTransform3D view = new AffineTransform3D();
		bdv.getViewerPanel().getState().getViewerTransform( view );
		Utils.log( view.toString().replace( "3d-affine", "View" )  );
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

//	private JComboBox< Double > createResolutionComboBox()
//	{
//		final JComboBox< Double > resolutionComboBox = new JComboBox( );
//
//		final ArrayList< Double > resolutions = new ArrayList<>();
//		resolutions.add( 0.25 );
//		resolutions.add( 0.10 );
//		resolutions.add( 0.05 );
//		resolutions.add( 0.01 );
//
//		for ( double resolution : resolutions )
//		{
//			resolutionComboBox.addItem( resolution );
//		}
//
//		resolutionComboBox.setSelectedIndex( 0 );
//
//		return resolutionComboBox;
//	}

	private void addLocalGeneSearchBehaviour()
	{
		geneSearchRadiusInMicrometer = 3;

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
		{
			double[] micrometerPosition = new double[ 3 ];
			BdvUtils.getGlobalMouseCoordinates( bdv ).localize( micrometerPosition );

			final BdvTextOverlay bdvTextOverlay
					= new BdvTextOverlay( bdv,
					"Searching expressed genes; please wait...", micrometerPosition );

			new Thread( () ->
			{
				searchGenes( micrometerPosition, geneSearchRadiusInMicrometer );
				bdvTextOverlay.setText( "" );
			}
			).start();

		}, "discover genes", "D" );

	}

	public void searchGenes( double[] micrometerPosition, double micrometerRadius )
	{
		GeneSearch geneSearch = new GeneSearch(
				micrometerRadius,
				micrometerPosition,
				sourcesPanel.getImageSourcesModel() );

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

//	private int getAppropriateLevel( double radius, double scale, double[][] resolutions )
//	{
//		int appropriateLevel = 0;
//		for( int level = 0; level < resolutions.length; ++level )
//		{
//			double levelBinning = resolutions[ level ][ 0 ];
//			if ( levelBinning * scale > radius )
//			{
//				appropriateLevel = level - 1;
//				break;
//			}
//		}
//		return appropriateLevel;
//	}

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
//		horizontalLayoutPanel.add( changeReference );

		final JButton defaultReference = new JButton( "Set default level vector" );
//		horizontalLayoutPanel.add( defaultReference );

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

	private void addMoveToViewUI( JPanel panel )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton moveToButton = new JButton( "Move to" );

		final String[] positionsAndViews = {
				"...type here...                                                           ",
				PlatyViews.LEFT_EYE_POSITION  };
		final JComboBox< String > viewsChoices = new JComboBox<>( positionsAndViews );
		viewsChoices.setEditable( true );
		viewsChoices.setMaximumSize( new Dimension( 200, TEXT_FIELD_HEIGHT ) );
		viewsChoices.setMinimumSize( new Dimension(  200, TEXT_FIELD_HEIGHT ) );

		moveToButton.addActionListener( e -> BdvViewChanger.moveToView( bdv, (String) viewsChoices.getSelectedItem()) );

		horizontalLayoutPanel.add( moveToButton );
		horizontalLayoutPanel.add( viewsChoices );
		panel.add( horizontalLayoutPanel );
	}


}
