package de.embl.cba.mobie.ui.viewer;

import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.mobie.bookmark.Location;
import de.embl.cba.mobie.platybrowser.GeneSearch;
import de.embl.cba.mobie.platybrowser.GeneSearchResults;
import de.embl.cba.mobie.bookmark.BookmarkManager;
import de.embl.cba.mobie.bdv.BdvViewChanger;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.tables.SwingUtils;
import ij3d.Image3DUniverse;
import org.scijava.java3d.Transform3D;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.util.Behaviours;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.*;

import static de.embl.cba.mobie.utils.ui.SwingUtils.*;

public class UserInterfaceProvider extends JPanel
{
	public static final String BUTTON_LABEL_VIEW = "view";
	public static final String BUTTON_LABEL_MOVE = "move";
	public static final String BUTTON_LABEL_HELP = "show";
	public static final String BUTTON_LABEL_SWITCH = "switch";
	public static final String BUTTON_LABEL_LEVEL = "level";
	public static final String BUTTON_LABEL_ADD = "add";

	private final SourcesManager sourcesManager;
	private BdvHandle bdv;
	private final MoBIEViewer moBIEViewer;
	private final ArrayList< String > datasets;
	private final BookmarkManager bookmarkManager;
	private Behaviours behaviours;

	private double[] levelingVector;
	private double[] targetNormalVector;
	private HashMap< String, String > selectionNameAndModalityToSourceName;
	private ArrayList< String > sortedModalities;
	private final String projectLocation;
	private JPanel sourcesSelectionPanel;

	public UserInterfaceProvider( MoBIEViewer moBIEViewer )
	{
		this.moBIEViewer = moBIEViewer;
		this.sourcesManager = moBIEViewer.getSourcesManager();
		this.datasets = moBIEViewer.getDatasets();
		this.bookmarkManager = moBIEViewer.getBookmarkManager();
		this.levelingVector = moBIEViewer.getLevelingVector();
		this.projectLocation = moBIEViewer.getProjectLocation();

		addInfoUI( this, projectLocation, moBIEViewer.getOptions().values.getPublicationURL() );
		this.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		addDatasetSelectionUI( this );
		this.add( new JSeparator( SwingConstants.HORIZONTAL ) );

		createSourceSelectionPanel();

		this.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		bookmarkManager.setBookmarkDropDown( addBookmarksUI( this  ) );
		addMoveToLocationUI( this );
		addLevelingUI( this );
		configPanel();
	}

	public void addUserInterfaceToBDV( BdvHandle bdv )
	{
		this.bdv = bdv;
		installBdvBehavioursAndPopupMenu( bdv, bookmarkManager, projectLocation );

		bdv.getCardPanel().addCard( "MoBIE Sources", sourcesSelectionPanel, true, new Insets( 4, 4, 0, 0 ) );
	}



	private void configPanel()
	{
		this.setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
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

	// TODO: move to gene search
	public void searchGenes( double[] micrometerPosition, double micrometerRadius )
	{
		GeneSearch geneSearch = new GeneSearch(
				micrometerRadius,
				micrometerPosition,
				sourcesManager.getSourcesModel() );

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
		final Image3DUniverse universe = sourcesManager.getUniverse();

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
		final Image3DUniverse universe = sourcesManager.getUniverse();

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

	private void createSourceSelectionPanel( )
	{
		selectionNameAndModalityToSourceName = new HashMap<>();
		HashMap< String, ArrayList< String > > modalityToSelectionNames = new HashMap<>();

		for ( String sourceName : sourcesManager.getSourceNames() )
		{
			String modality = sourceName.split( "-" )[ 0 ];

			String selectionName = sourceName.replace( modality + "-", "" );

			final Metadata metadata = sourcesManager.getSourceAndDefaultMetadata( sourceName ).metadata();

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

		sourcesSelectionPanel = new JPanel( new BorderLayout() );
		sourcesSelectionPanel.setLayout( new BoxLayout( sourcesSelectionPanel, BoxLayout.Y_AXIS ) );
		for ( String modality : sortedModalities )
		{
			final String[] names = Utils.getSortedList( modalityToSelectionNames.get( modality ) ).toArray( new String[ 0 ] );
			final JComboBox< String > comboBox = new JComboBox<>( names );
			setComboBoxDimensions( comboBox );
			addSourceSelectionComboBoxAndButton( sourcesSelectionPanel, comboBox, modality );
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

				sourcesManager.addSourceToPanelAndViewer( sourceName );
			} );
		} );


		final JLabel comp = getJLabel( modality );

		horizontalLayoutPanel.add( comp );
		horizontalLayoutPanel.add( comboBox );
		horizontalLayoutPanel.add( button );

		panel.add( horizontalLayoutPanel );
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

	private JComboBox<String> addBookmarksUI( JPanel panel )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton button = getButton( BUTTON_LABEL_VIEW );

		final String[] bookmarkNames = getBookmarkNames();

		final JComboBox< String > comboBox = new JComboBox<>( bookmarkNames );
		setComboBoxDimensions( comboBox );
		button.addActionListener( e -> bookmarkManager.setView( ( String ) comboBox.getSelectedItem() ) );

		horizontalLayoutPanel.add( getJLabel( "bookmark" ) );
		horizontalLayoutPanel.add( comboBox );
		horizontalLayoutPanel.add( button );

		panel.add( horizontalLayoutPanel );

		return comboBox;
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

	private void addInfoUI( JPanel panel, String projectLocation, String publicationURL )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton button = getButton( BUTTON_LABEL_HELP );

		final MoBIEInfo moBIEInfo = new MoBIEInfo( projectLocation, publicationURL );

		final JComboBox< String > comboBox = new JComboBox<>( moBIEInfo.getInfoChoices() );
		setComboBoxDimensions( comboBox );

		button.addActionListener( e -> {
			moBIEInfo.showInfo( ( String ) comboBox.getSelectedItem() );
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
		final URL resource = UserInterfaceProvider.class.getResource( "/mobie.jpeg" );
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
		new MoBIEViewer( moBIEViewer.getProjectLocation(), moBIEViewer.getOptions().dataset( dataset ) );
	}

	// TODO simplify code below
	private String[] getBookmarkNames()
	{
		final Set< String > viewNames = bookmarkManager.getBookmarkNames();
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
