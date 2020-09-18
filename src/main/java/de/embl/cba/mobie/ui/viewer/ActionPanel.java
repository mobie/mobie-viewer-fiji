package de.embl.cba.mobie.ui.viewer;

import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.bdv.utils.popup.BdvPopupMenus;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.mobie.bookmark.Location;
import de.embl.cba.mobie.bookmark.LocationType;
import de.embl.cba.mobie.platybrowser.GeneSearch;
import de.embl.cba.mobie.platybrowser.GeneSearchResults;
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
import java.net.URL;
import java.util.*;

public class ActionPanel extends JPanel
{
	public static final int TEXT_FIELD_HEIGHT = 20;
	public static final int COMBOBOX_WIDTH = 270;
	public static final String BUTTON_LABEL_VIEW = "view";
	public static final String BUTTON_LABEL_MOVE = "move";
	public static final String BUTTON_LABEL_HELP = "info";
	public static final String BUTTON_LABEL_SWITCH = "switch";
	public static final Dimension BUTTON_DIMENSION = new Dimension( 80, TEXT_FIELD_HEIGHT );
	public static final String BUTTON_LABEL_LEVEL = "level";
	public static final String BUTTON_LABEL_ADD = "add";
	public static final String RESTORE_DEFAULT_VIEW_TRIGGER = "ctrl R";

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
	private final String projectLocation;

	public ActionPanel( MoBIEViewer moBIEViewer )
	{
		this.moBIEViewer = moBIEViewer;
		this.sourcesPanel = moBIEViewer.getSourcesPanel();
		this.datasets = moBIEViewer.getDatasets();
		this.bookmarksManager = moBIEViewer.getBookmarksManager();
		this.levelingVector = moBIEViewer.getLevelingVector();
		this.projectLocation = moBIEViewer.getProjectLocation();

		addInfoUI( this, projectLocation );
		this.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		addDatasetSelectionUI( this );
		this.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		addSourceSelectionUI( this );
		this.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		addBookmarksUI( this  );
		addMoveToLocationUI( this );
		addLevelingUI( this );
		configPanel();
	}

	public void setBdvAndInstallBehavioursAndPopupMenu( BdvHandle bdv )
	{
		this.bdv = bdv;
		installBdvBehavioursAndPopupMenu();
	}

	public double getGeneSearchRadiusInMicrometer()
	{
		return geneSearchRadiusInMicrometer;
	}

	public void setGeneSearchRadiusInMicrometer( double geneSearchRadiusInMicrometer )
	{
		this.geneSearchRadiusInMicrometer = geneSearchRadiusInMicrometer;
	}

	private void installBdvBehavioursAndPopupMenu()
	{
		BdvPopupMenus.addScreenshotAction( bdv );

		BdvPopupMenus.addAction( bdv, "Log Current Location",
				() -> {
					(new Thread( () -> {
						Logger.log( "\nPosition:\n" + BdvUtils.getGlobalMousePositionString( bdv ) );
						Logger.log( "View:\n" + BdvUtils.getBdvViewerTransformString( bdv ) );
						Logger.log( "Normalised view:\n" + Utils.createNormalisedViewerTransformString( bdv, Utils.getMousePosition( bdv ) ) );
					} )).start();
				});

		BdvPopupMenus.addAction( bdv, "Restore Default View" + BdvUtils.getShortCutString( RESTORE_DEFAULT_VIEW_TRIGGER ) ,
				() -> {
					(new Thread( () -> {
						restoreDefaultView();
					} )).start();

				});

		if ( projectLocation.contains( "platybrowser" ) )
		{
			BdvPopupMenus.addAction( bdv, "Search Genes...", ( x, y ) ->
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
			} );
		}

		behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdv.getTriggerbindings(), "behaviours" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			(new Thread( () -> {
				restoreDefaultView();
			} )).start();
		}, "Toggle point overlays", RESTORE_DEFAULT_VIEW_TRIGGER ) ;

		//addLocalGeneSearchBehaviour();
		//BdvBehaviours.addPositionAndViewLoggingBehaviour( bdv, behaviours, "P" );
		//BdvBehaviours.addViewCaptureBehaviour( bdv, behaviours, "C", false );
		//BdvBehaviours.addViewCaptureBehaviour( bdv, behaviours, "shift C", true );
	}

	private void restoreDefaultView()
	{
		final Location location = new Location( LocationType.NormalisedViewerTransform, moBIEViewer.getDefaultNormalisedViewerTransform().getRowPackedCopy() );
		BdvViewChanger.moveToLocation( bdv, location );
	}

	private void configPanel()
	{
		this.setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
		//this.revalidate();
		//this.repaint();
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

		final JButton button = getButton( BUTTON_LABEL_ADD );
		button.addActionListener( e ->
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
		horizontalLayoutPanel.add( button );

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

		final JButton button = getButton( BUTTON_LABEL_LEVEL );
		horizontalLayoutPanel.add( button );

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

		button.addActionListener( e -> BdvUtils.levelCurrentView( bdv, targetNormalVector ) );

		panel.add( horizontalLayoutPanel );
	}

	private void addBookmarksUI( JPanel panel )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton button = getButton( BUTTON_LABEL_VIEW );

		final String[] bookmarkNames = getBookmarkNames();

		final JComboBox< String > comboBox = new JComboBox<>( bookmarkNames );
		setComboBoxDimensions( comboBox );
		button.addActionListener( e -> bookmarksManager.setView( ( String ) comboBox.getSelectedItem() ) );

		horizontalLayoutPanel.add( getJLabel( "bookmark" ) );
		horizontalLayoutPanel.add( comboBox );
		horizontalLayoutPanel.add( button );

		panel.add( horizontalLayoutPanel );
	}

	private JButton getButton( String buttonLabel )
	{
		final JButton button = new JButton( buttonLabel );
		button.setPreferredSize( BUTTON_DIMENSION ); // TODO
		return button;
	}

	private void addMoveToLocationUI( JPanel panel )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton button = getButton( BUTTON_LABEL_MOVE );

		final JTextField jTextField = new JTextField( "120.5,115.3,201.5" );
		jTextField.setPreferredSize( new Dimension( COMBOBOX_WIDTH - 3, TEXT_FIELD_HEIGHT ) );
		jTextField.setMaximumSize( new Dimension( COMBOBOX_WIDTH - 3, TEXT_FIELD_HEIGHT ) );
		button.addActionListener( e -> BdvViewChanger.moveToLocation( bdv, new Location( jTextField.getText() ) ) );

		horizontalLayoutPanel.add( getJLabel( "location" ) );
		horizontalLayoutPanel.add( jTextField );
		horizontalLayoutPanel.add( button );

		panel.add( horizontalLayoutPanel );
	}

	private void addInfoUI( JPanel panel, String projectLocation )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton button = getButton( BUTTON_LABEL_HELP );

		final JComboBox< String > comboBox = new JComboBox<>( MoBIEInfo.getInfoChoices() );
		setComboBoxDimensions( comboBox );

		button.addActionListener( e -> {
			final MoBIEInfo mobIEInfo = new MoBIEInfo( projectLocation );
			mobIEInfo.showInfo( ( String ) comboBox.getSelectedItem() );
		} );
		comboBox.setPrototypeDisplayValue( MoBIEViewer.PROTOTYPE_DISPLAY_VALUE  );

		horizontalLayoutPanel.setSize( 0, 80 );
		final ImageIcon icon = getMobieIcon( 80 );
		final JLabel moBIE = new JLabel( "                   " );
		moBIE.setIcon( icon );

		horizontalLayoutPanel.add( moBIE );
		horizontalLayoutPanel.add( comboBox );
		horizontalLayoutPanel.add( button );

		panel.add( horizontalLayoutPanel );
	}

	private ImageIcon getMobieIcon( int size )
	{
		final URL resource = ActionPanel.class.getResource( "/mobie.jpeg" );
		final ImageIcon imageIcon = new ImageIcon( resource );
		final Image scaledInstance = imageIcon.getImage().getScaledInstance( size, size, Image.SCALE_SMOOTH );
		return new ImageIcon( scaledInstance );
	}

	private void addDatasetSelectionUI( JPanel panel )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton button = getButton( BUTTON_LABEL_SWITCH );

		final String[] choices = datasets.stream().toArray( String[]::new );
		final JComboBox< String > comboBox = new JComboBox<>( choices );
		comboBox.setSelectedItem( moBIEViewer.getDataset() );
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
		// TODO: make sure the Swing UI (sources panel is fully visible before instantiating the new BDV)
		moBIEViewer.close();
		new MoBIEViewer( moBIEViewer.getProjectImagesLocation(), moBIEViewer.getProjectTablesLocation(), new ViewerOptions().dataset( dataset ) );
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
		BdvViewChanger.moveToLocation( bdv, new Location( view ) );
	}

}
