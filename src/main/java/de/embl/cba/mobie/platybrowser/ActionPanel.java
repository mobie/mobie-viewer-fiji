package de.embl.cba.mobie.platybrowser;

import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.bdv.utils.behaviour.BdvBehaviours;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.mobie.GeneSearch;
import de.embl.cba.mobie.GeneSearchResults;
import de.embl.cba.mobie.bookmark.BookmarksManager;
import de.embl.cba.mobie.bdv.BdvViewChanger;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.mobie.utils.ui.BdvTextOverlay;
import de.embl.cba.tables.SwingUtils;
import ij3d.Image3DUniverse;
import org.scijava.java3d.Transform3D;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class ActionPanel extends JPanel
{
	public static final int TEXT_FIELD_HEIGHT = 20;
	public static final int COMBOBOX_WIDTH = 270;

	private final SourcesPanel sourcesPanel;
	private BdvHandle bdv;
	private final MoBIEViewer moBIEViewer;
	private final ArrayList< String > datasets;
	private final BookmarksManager bookmarksManager;
	private Behaviours behaviours;

	private double[] levelingVector;
	private double[] targetNormalVector;
	private double geneSearchRadiusInMicrometer;
	private HashMap< String, String > selectionNameAndModalityToSourceName;
	private ArrayList< String > sortedModalities;

	public ActionPanel( MoBIEViewer moBIEViewer )
	{
		this.moBIEViewer = moBIEViewer;
		this.sourcesPanel = moBIEViewer.getSourcesPanel();
		this.datasets = moBIEViewer.getDatasets();
		this.bookmarksManager = moBIEViewer.getBookmarksManager();
		this.levelingVector = moBIEViewer.getLevelingVector();

		this.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		addHelpUI( this );
		this.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		addDatasetSelectionUI( this );
		this.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		addSourceSelectionUI( this );
		this.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		addBookmarksUI( this  );
		addMoveToUI( this );
		addLevelingUI( this );
		//this.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		//addShow3DUI( this );
		configPanel();
	}

	public void setBdv( BdvHandle bdv )
	{
		this.bdv = bdv;
		installBdvBehaviours();
	}

//	private void addShow3DUI( JPanel panel )
//	{
//		JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();
////		horizontalLayoutPanel.setAlignmentX( Component.CENTER_ALIGNMENT  );
//
//		final JCheckBox cbObjects = new JCheckBox( "show objects in 3D" );
//		cbObjects.setSelected( Globals.showSegmentsIn3D.get() );
//		cbObjects.addActionListener( e -> {
//			Globals.showSegmentsIn3D.set( cbObjects.isSelected() );
//		} );
//		cbObjects.setHorizontalAlignment( SwingConstants.CENTER );
//
//		final JCheckBox cbVolumes = new JCheckBox( "show volumes in 3D" );
//		cbVolumes.setSelected( Globals.showVolumesIn3D.get() );
//		cbVolumes.addActionListener( e -> {
//			Globals.showVolumesIn3D.set( cbVolumes.isSelected() );
//		} );
//		cbVolumes.setHorizontalAlignment( SwingConstants.CENTER );
//
//		horizontalLayoutPanel.add( Box.createHorizontalGlue() );
//		horizontalLayoutPanel.add( cbObjects );
//		horizontalLayoutPanel.add( Box.createHorizontalGlue() );
//		horizontalLayoutPanel.add( cbVolumes );
//		horizontalLayoutPanel.add( Box.createHorizontalGlue() );
//		panel.add( horizontalLayoutPanel );
//	}

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
		addPointOverlayTogglingBehaviour();
		addLocalGeneSearchBehaviour();
		BdvBehaviours.addPositionAndViewLoggingBehaviour( bdv, behaviours, "P" );
		BdvBehaviours.addViewCaptureBehaviour( bdv, behaviours, "C", false );
		BdvBehaviours.addViewCaptureBehaviour( bdv, behaviours, "shift C", true );
//		BdvBehaviours.addSimpleViewCaptureBehaviour( bdv, behaviours, "C" );
	}

	private void configPanel()
	{
		this.setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
		this.revalidate();
		this.repaint();
	}

	private void addPointOverlayTogglingBehaviour(  )
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			(new Thread( () -> {
				BdvViewChanger.togglePointOverlay();
			} )).start();
		}, "Toggle point overlays", "ctrl P"  ) ;
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

		final Map< String, Double > geneExpressionLevels = geneSearch.runSearchAndGetLocalExpression();

		GeneSearchResults.addRowToGeneExpressionTable(
				micrometerPosition, micrometerRadius, geneExpressionLevels );

		GeneSearchResults.logGeneExpression(
				micrometerPosition, micrometerRadius, geneExpressionLevels );
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

		if ( universe == null ) return "";

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
		selectionNameAndModalityToSourceName = new HashMap<>();
		HashMap< String, ArrayList< String > > modalityToSelectionNames = new HashMap<>();

		for ( String sourceName : sourcesPanel.getSourceNames() )
		{
			String modality = sourceName.split( "-" )[ 0 ];

			String selectionName = sourceName.replace( modality + "-", "" );

			final Metadata metadata = sourcesPanel.getSourceAndMetadata( sourceName ).metadata();

			if ( metadata.type.equals( Metadata.Type.Segmentation ) )
			{
				if ( ! modality.contains( " segmentation" ) )
					modality += " segmentation";
			}
			else if ( metadata.type.equals( Metadata.Type.Mask ) )
			{
				if ( ! modality.contains( " segmentation" ) )
					modality += " segmentation";
			}

			selectionName = Utils.getSimplifiedSourceName( selectionName, false );

			selectionNameAndModalityToSourceName.put( selectionName + "-" + modality, sourceName  );

			if ( ! modalityToSelectionNames.containsKey( modality ) )
				modalityToSelectionNames.put( modality, new ArrayList< String >(  ) );

			modalityToSelectionNames.get( modality ).add( selectionName);
		}

		sortedModalities = Utils.getSortedList( modalityToSelectionNames.keySet() );

		for ( String modality : sortedModalities )
		{
			final String[] names = Utils.getSortedList( modalityToSelectionNames.get( modality ) ).toArray( new String[ 0 ] );
			final JComboBox< String > comboBox = new JComboBox<>( names );
			setComboBoxDimensions( comboBox );
			addSourceSelectionComboBoxAndButton( panel, comboBox, modality );
		}
	}

	public ArrayList< String > getSortedModalities()
	{
		return sortedModalities;
	}

	private void addSourceSelectionComboBoxAndButton(
			final JPanel panel,
			final JComboBox comboBox,
			final String modality )
	{
		if ( comboBox.getModel().getSize() == 0 ) return;

		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton addToView = new JButton( "add");
		addToView.addActionListener( e ->
		{
			SwingUtilities.invokeLater( () ->
			{
				if ( bdv == null )
				{
					Logger.log( "Warning: Source cannot be added yet, because BigDataViewer is still being initialised..." );
					return;
				}

				final String selectedSource = ( String ) comboBox.getSelectedItem();
				final String sourceName = selectionNameAndModalityToSourceName.get( selectedSource + "-" + modality );

				sourcesPanel.addSourceToPanelAndViewer( sourceName );
			} );
		} );


		final JLabel comp = getJLabel( modality );

		horizontalLayoutPanel.add( comp );
		horizontalLayoutPanel.add( comboBox );
		horizontalLayoutPanel.add( addToView );

		panel.add( horizontalLayoutPanel );
	}

	private JLabel getJLabel( String text )
	{
		final JLabel comp = new JLabel( text );
		comp.setPreferredSize( new Dimension( 170,10 ) );
		comp.setHorizontalAlignment( SwingConstants.LEFT );
		comp.setHorizontalTextPosition( SwingConstants.LEFT );
		comp.setAlignmentX( Component.LEFT_ALIGNMENT );
		return comp;
	}

	private void addLevelingUI( JPanel panel )
	{
		if ( levelingVector == null ) return;

		this.targetNormalVector = Arrays.copyOf( levelingVector, 3 );

		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton levelCurrentView = new JButton( "level" );
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
			targetNormalVector = Arrays.copyOf( levelingVector, 3);
			Utils.logVector( "New reference normal vector (default): ", levelingVector );
		} );

		levelCurrentView.addActionListener( e -> BdvUtils.levelCurrentView( bdv, targetNormalVector ) );

		panel.add( horizontalLayoutPanel );
	}

	private void addBookmarksUI( JPanel panel )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton viewButton = new JButton( "view" );

		final String[] bookmarkNames = getBookmarkNames();

		final JComboBox< String > comboBox = new JComboBox<>( bookmarkNames );
		setComboBoxDimensions( comboBox );
		viewButton.addActionListener( e -> bookmarksManager.setView( ( String ) comboBox.getSelectedItem() ) );

		horizontalLayoutPanel.add( getJLabel( "bookmark" ) );
		horizontalLayoutPanel.add( comboBox );
		horizontalLayoutPanel.add( viewButton );

		panel.add( horizontalLayoutPanel );
	}

	private void addMoveToUI( JPanel panel )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton button = new JButton( "move" );
		final JTextField jTextField = new JTextField( "120.5,115.3,201.5" );
		jTextField.setPreferredSize( new Dimension( COMBOBOX_WIDTH - 3, 20 ) );
		jTextField.setMaximumSize( new Dimension( COMBOBOX_WIDTH - 3, 20 ) );
		button.addActionListener( e -> BdvViewChanger.moveToView( bdv, jTextField.getText() ) );

		horizontalLayoutPanel.add( getJLabel( "position" ) );
		horizontalLayoutPanel.add( jTextField );
		horizontalLayoutPanel.add( button );

		panel.add( horizontalLayoutPanel );
	}

	private void addHelpUI( JPanel panel )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton button = new JButton( "help" );

		final String[] choices = {
				PlatyBrowserHelp.BIG_DATA_VIEWER,
				PlatyBrowserHelp.PLATY_BROWSER,
				PlatyBrowserHelp.SEGMENTATIONS };
		final JComboBox< String > comboBox = new JComboBox<>( choices );
		setComboBoxDimensions( comboBox );
		button.addActionListener( e -> PlatyBrowserHelp.showHelp( ( String ) comboBox.getSelectedItem() ) );
		comboBox.setPrototypeDisplayValue( MoBIEViewer.PROTOTYPE_DISPLAY_VALUE  );

		horizontalLayoutPanel.add( getJLabel( " " ) );
		horizontalLayoutPanel.add( comboBox );
		horizontalLayoutPanel.add( button );

		panel.add( horizontalLayoutPanel );
	}

	private void addDatasetSelectionUI( JPanel panel )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton button = new JButton( "switch" );

		final String[] choices = datasets.stream().toArray( String[]::new );
		final JComboBox< String > comboBox = new JComboBox<>( choices );
		setComboBoxDimensions( comboBox );
		button.addActionListener( e -> switchDataset( ( String ) comboBox.getSelectedItem() ) );
		comboBox.setPrototypeDisplayValue( MoBIEViewer.PROTOTYPE_DISPLAY_VALUE  );

		horizontalLayoutPanel.add( getJLabel( "dataset" ) );
		horizontalLayoutPanel.add( comboBox );
		horizontalLayoutPanel.add( button );

		panel.add( horizontalLayoutPanel );
	}

	private void switchDataset( String dataset )
	{
		new MoBIEViewer( moBIEViewer.getProjectImagesLocation(), moBIEViewer.getProjectTablesLocation(), new ViewerOptions().dataset( dataset ) );
		moBIEViewer.close();
	}

	private void setComboBoxDimensions( JComboBox< String > comboBox )
	{
		comboBox.setPrototypeDisplayValue( MoBIEViewer.PROTOTYPE_DISPLAY_VALUE );
		comboBox.setPreferredSize( new Dimension( COMBOBOX_WIDTH, 20 ) );
		comboBox.setMaximumSize( new Dimension( COMBOBOX_WIDTH, 20 ) );
	}

	// TODO simplify code below
	private String[] getBookmarkNames()
	{
		final Set< String > viewNames = bookmarksManager.getBookmarkNames();
		final String[] positionsAndViews = new String[ viewNames.size() ];
//		positionsAndViews[ 0 ] = "...type here...";
		final Iterator< String > iterator = viewNames.iterator();
		for ( int i = 0; i < viewNames.size() ; i++ )
			positionsAndViews[ i ] = iterator.next();
		return positionsAndViews;
	}

	public void setView( String view )
	{
		BdvViewChanger.moveToView( bdv, view );
	}

}
