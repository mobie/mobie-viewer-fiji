package de.embl.cba.platynereis.platybrowser;

import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.capture.BdvViewCaptures;
import de.embl.cba.bdv.utils.capture.PixelSpacingDialog;
import de.embl.cba.platynereis.GeneSearch;
import de.embl.cba.platynereis.GeneSearchResults;
import de.embl.cba.platynereis.Globals;
import de.embl.cba.platynereis.utils.BdvViewChanger;
import de.embl.cba.platynereis.utils.SortIgnoreCase;
import de.embl.cba.platynereis.utils.Utils;
import de.embl.cba.platynereis.utils.ui.BdvTextOverlay;
import de.embl.cba.tables.SwingUtils;
import ij3d.Image3DUniverse;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.java3d.Transform3D;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class PlatyBrowserActionPanel extends JPanel
{
	public static final int TEXT_FIELD_HEIGHT = 20;

	private final PlatyBrowserSourcesPanel sourcesPanel;
	private BdvHandle bdv;
	private Behaviours behaviours;

	private double[] defaultTargetNormalVector = new double[]{0.70,0.56,0.43};
	private double[] targetNormalVector;
	private double geneSearchRadiusInMicrometer;
	private HashMap< String, String > selectionNameAndModalityToSourceName;

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

		final JCheckBox cbObjects = new JCheckBox( "Show objects in 3D" );
		cbObjects.setSelected( Globals.showSegmentsIn3D.get() );
		cbObjects.addActionListener( e -> {
			Globals.showSegmentsIn3D.set( cbObjects.isSelected() );
		} );

		final JCheckBox cbVolumes = new JCheckBox( "Show volumes in 3D" );
		cbVolumes.setSelected( Globals.showVolumesIn3D.get() );
		cbVolumes.addActionListener( e -> {
			Globals.showVolumesIn3D.set( cbVolumes.isSelected() );
		} );

		horizontalLayoutPanel.add( cbObjects );
		horizontalLayoutPanel.add( cbVolumes );
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
		addPointOverlayTogglingBehaviour();
		addLocalGeneSearchBehaviour();
		addViewCaptureBehaviour();
	}

	private void configPanel()
	{
		this.setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
		this.revalidate();
		this.repaint();
	}

	public void addViewCaptureBehaviour()
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
		{
			new Thread( () -> {
				final String pixelUnit = "micrometer";
				final PixelSpacingDialog dialog = new PixelSpacingDialog( BdvUtils.getViewerVoxelSpacing( bdv )[ 0 ], pixelUnit );
				if ( ! dialog.showDialog() ) return;
				Utils.log( "Loading data to capture current view..." );
				BdvViewCaptures.captureView(
						bdv,
						dialog.getPixelSpacing(),
						pixelUnit,
						false );
			}).start();
		}, "capture view", "C" ) ;
	}

	private void addPointOverlayTogglingBehaviour(  )
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			(new Thread( () -> {
				BdvViewChanger.togglePointOverlay();
			} )).start();
		}, "Toggle point overlays", "ctrl P"  ) ;
	}

	private void addPositionAndViewLoggingBehaviour( JPanel panel )
	{
		JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {

			(new Thread( () -> {
				logPositionAndViews();
			} )).start();

		}, "Print position and view", "P"  ) ;

		panel.add( horizontalLayoutPanel );
	}

	private void logPositionAndViews()
	{
		final RealPoint globalMouseCoordinates = BdvUtils.getGlobalMouseCoordinates( bdv );
		Utils.log( "\nPosition: \n" + globalMouseCoordinates.toString() );

		Utils.log( "BigDataViewer transform: \n"+ getBdvViewerTransform() );
		Utils.log( "3D Viewer transform: \n" + getVolumeViewerTransform() );
	}

	private String getBdvViewerTransform()
	{
		final AffineTransform3D view = new AffineTransform3D();
		bdv.getViewerPanel().getState().getViewerTransform( view );

		return view.toString().replace( "3d-affine", "View" );
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


	// TODO: refactor to UniverseUtils
	private HashMap< String, String > getTransforms( String view )
	{
		final HashMap<String, String> transforms = new HashMap<String, String>();

		final String[] lines = view.split( "\n" );

		for ( String line : lines )
		{
			final String[] keyval = line.split("=");
			transforms.put(keyval[0].trim(), keyval[1].trim());
		}
		return transforms;
	}

	// TODO: refactor to UniverseUtils
	public void setVolumeView( String view )
	{
		setVolumeView( getTransforms( view ) );
	}

	// TODO: refactor to UniverseUtils
	public void setVolumeView( HashMap<String, String> transforms)
	{
		final Image3DUniverse universe = sourcesPanel.getUniverse();

		String tmp;
		// Set up new Content
		if ((tmp = transforms.get("center")) != null) universe.getCenterTG().setTransform(
				t(tmp));
		if ((tmp = transforms.get("translate")) != null) universe.getTranslateTG()
				.setTransform(t(tmp));
		if ((tmp = transforms.get("rotate")) != null) universe.getRotationTG().setTransform(
				t(tmp));
		if ((tmp = transforms.get("zoom")) != null) universe.getZoomTG()
				.setTransform(t(tmp));
		if ((tmp = transforms.get("animate")) != null) universe.getAnimationTG()
				.setTransform(t(tmp));

		universe.getViewPlatformTransformer().updateFrontBackClip();
	}

	// TODO: refactor to UniverseUtils
	private static final Transform3D t( final String s) {
		final String[] sp = s.split(" ");
		final float[] f = new float[16];
		for (int i = 0; i < sp.length; i++)
			f[i] = f(sp[i]);
		return new Transform3D(f);
	}

	// TODO: refactor to UniverseUtils
	private static final float f(final String s) {
		return Float.parseFloat(s);
	}

	String getVolumeViewerTransform()
	{
		final Image3DUniverse universe = sourcesPanel.getUniverse();
		String transform = "";

		final Transform3D t3d = new Transform3D();
		universe.getCenterTG().getTransform(t3d);
		transform += "center = " + toString(t3d) +"\n";
		universe.getTranslateTG().getTransform(t3d);
		transform += "translate = " + toString(t3d) +"\n";
		universe.getRotationTG().getTransform(t3d);
		transform += "rotate = " + toString(t3d) +"\n";
		universe.getZoomTG().getTransform(t3d);
		transform += "zoom = " + toString(t3d) +"\n";
		universe.getAnimationTG().getTransform(t3d);
		transform += "animate = " + toString(t3d) +"\n";

		return transform;
	}

	private static final String toString(final Transform3D t3d) {
		final float[] xf = new float[16];
		t3d.get(xf);
		String ret = "";
		for (int i = 0; i < 16; i++)
			ret += " " + xf[i];
		return ret;
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
		final HashMap< String, JComboBox > modalityToJComboBox = new HashMap<>();

		selectionNameAndModalityToSourceName = new HashMap<>();
		HashMap< String, ArrayList< String > > modalityToSelectionNames = new HashMap<>();

		for ( String sourceName : sourcesPanel.getSourceNames() )
		{
			String modality = sourceName.split( "-" )[ 0 ];

			String selectionName = sourceName.replace( modality + "-", "" );

			if ( sourceName.contains( "segmented" ) )
			{
				selectionName = selectionName.replace( "segmented-", "" );
				if ( ! modality.contains( " segmentation" ) )
					modality += " segmentation";
			}

			if ( sourceName.contains( "mask" ) )
			{
				selectionName = selectionName.replace( "mask-", "" );
				if ( ! modality.contains( " segmentation" ) )
					modality += " segmentation";
			}

			if ( sourceName.contains( "labels" ) )
			{
				selectionName = selectionName.replace( "labels-", "" );
				if ( ! modality.contains( " segmentation" ) )
					modality += " segmentation";
			}

			selectionName = selectionName.replace(  "6dpf-1-whole-", "");

			selectionNameAndModalityToSourceName.put( selectionName + "-" + modality, sourceName  );

			if ( ! modalityToSelectionNames.containsKey( modality ) )
				modalityToSelectionNames.put( modality, new ArrayList< String >(  ) );

			modalityToSelectionNames.get( modality ).add( selectionName);

		}

		final ArrayList< String > sortedModalities = getSortedList( modalityToSelectionNames.keySet() );

		for ( String modality : sortedModalities )
		{
			final String[] names = getSortedList( modalityToSelectionNames.get( modality ) ).toArray( new String[ 0 ] );
			final JComboBox< String > comboBox = new JComboBox<>( names );
			addSourceSelectionComboBoxAndButton( panel, comboBox, modality );
		}
	}

	private void addSourceSelectionComboBoxAndButton(
			final JPanel panel,
			final JComboBox comboBox,
			final String modality )
	{
		if ( comboBox.getModel().getSize() == 0 ) return;

		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton addToView = new JButton( "View " + modality );
		addToView.addActionListener( e ->
		{
			final String selectedSource = ( String ) comboBox.getSelectedItem();
			final String sourceName = selectionNameAndModalityToSourceName.get( selectedSource + "-" + modality );
			sourcesPanel.addSourceToPanelAndViewer( sourceName );
		} );

		horizontalLayoutPanel.add( addToView );
		horizontalLayoutPanel.add( comboBox );

		panel.add( horizontalLayoutPanel );
	}


	private ArrayList< String > getSortedList( Collection< String > strings )
	{
		final ArrayList< String > sorted = new ArrayList<>( strings );
		Collections.sort( sorted, new SortIgnoreCase() );
		return sorted;
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

		final String[] bookmarkNames = getBookmarkNames();

		final JComboBox< String > viewsChoices = new JComboBox<>( bookmarkNames );
		viewsChoices.setEditable( true );
		viewsChoices.setMaximumSize( new Dimension( 200, TEXT_FIELD_HEIGHT ) );
		viewsChoices.setMinimumSize( new Dimension(  200, TEXT_FIELD_HEIGHT ) );

		moveToButton.addActionListener( e -> setView( ( String ) viewsChoices.getSelectedItem() ) );

		horizontalLayoutPanel.add( moveToButton );
		horizontalLayoutPanel.add( viewsChoices );

		panel.add( horizontalLayoutPanel );
	}

	private String[] getBookmarkNames()
	{
		final Set< String > viewNames = BdvViewChanger.views.views().keySet();
		final String[] positionsAndViews = new String[ viewNames.size() + 1 ];
		positionsAndViews[ 0 ] = "...type here...                                                           ";
		final Iterator< String > iterator = viewNames.iterator();
		for ( int i = 1; i <= viewNames.size() ; i++ )
			positionsAndViews[ i ] = iterator.next();
		return positionsAndViews;
	}

	public void setView( String view )
	{
		BdvViewChanger.moveToView( bdv, view );
	}

}
