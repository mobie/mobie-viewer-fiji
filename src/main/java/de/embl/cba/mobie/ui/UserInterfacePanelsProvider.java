package de.embl.cba.mobie.ui;

import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.mobie.bdv.BdvViewChanger;
import de.embl.cba.mobie.bookmark.BookmarkManager;
import de.embl.cba.mobie.bookmark.Location;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.tables.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import static de.embl.cba.mobie.utils.ui.SwingUtils.*;

public class UserInterfacePanelsProvider
{
	public static final String BUTTON_LABEL_VIEW = "view";
	public static final String BUTTON_LABEL_MOVE = "move";
	public static final String BUTTON_LABEL_HELP = "show";
	public static final String BUTTON_LABEL_SWITCH = "switch";
	public static final String BUTTON_LABEL_LEVEL = "level";
	public static final String BUTTON_LABEL_ADD = "add";

	private final SourcesDisplayManager sourcesDisplayManager;
	private final MoBIE moBIE;
	private final ArrayList< String > datasets;
	private ArrayList< String > sortedModalities;
	private int sourceSelectionPanelHeight;

	public UserInterfacePanelsProvider( MoBIE moBIE )
	{
		this.moBIE = moBIE;
		this.sourcesDisplayManager = moBIE.getSourcesDisplayManager();
		this.datasets = moBIE.getDatasets();
	}

	// TODO: too complex, make own source selection class
	public JPanel createSourceSelectionPanel( SourcesDisplayManager sourcesDisplayManager )
	{
		HashMap< String, String > selectionNameAndModalityToSourceName = new HashMap<>();
		HashMap< String, ArrayList< String > > modalityToSelectionNames = new HashMap<>();

		for ( String sourceName : sourcesDisplayManager.getSourceNames() )
		{
			String modality = sourceName.split( "-" )[ 0 ];

			String selectionName = sourceName.replace( modality + "-", "" );

			final Metadata metadata = sourcesDisplayManager.getSourceAndDefaultMetadata( sourceName ).metadata();

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
				modalityToSelectionNames.put( modality, new ArrayList<>(  ) );

			modalityToSelectionNames.get( modality ).add( selectionName);
		}

		sortedModalities = Utils.getSortedList( modalityToSelectionNames.keySet() );

		JPanel sourcesSelectionPanel = new JPanel( new BorderLayout() );
		sourcesSelectionPanel.setLayout( new BoxLayout( sourcesSelectionPanel, BoxLayout.Y_AXIS ) );
		for ( String modality : sortedModalities )
		{
			final String[] names = Utils.getSortedList( modalityToSelectionNames.get( modality ) ).toArray( new String[ 0 ] );
			final JComboBox< String > comboBox = new JComboBox<>( names );
			setComboBoxDimensions( comboBox );
			addSourceSelectionComboBoxAndButton( selectionNameAndModalityToSourceName, sourcesSelectionPanel, comboBox, modality );
		}

		sourceSelectionPanelHeight = sortedModalities.size() * 40;

		return sourcesSelectionPanel;
	}

	public int getSourceSelectionPanelHeight()
	{
		return sourceSelectionPanelHeight;
	}

	private void addSourceSelectionComboBoxAndButton(
			HashMap< String, String > selectionNameAndModalityToSourceName,
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
				final String selectedSource = ( String ) comboBox.getSelectedItem();
				final String sourceName = selectionNameAndModalityToSourceName.get( selectedSource + "-" + modality );
				sourcesDisplayManager.addSourceToPanelAndViewer( sourceName );
			} );
		} );

		final JLabel comp = getJLabel( modality );

		horizontalLayoutPanel.add( comp );
		horizontalLayoutPanel.add( comboBox );
		horizontalLayoutPanel.add( button );

		panel.add( horizontalLayoutPanel );
	}

	public JPanel createLevelingPanel( BdvHandle bdvHandle, double[] levelingVector )
	{
		final double[] targetNormalVector = Arrays.copyOf( levelingVector, 3 );

		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton button = getButton( BUTTON_LABEL_LEVEL );
		horizontalLayoutPanel.add( button );

		// TODO: if below code is needed make an own Levelling class
//		final JButton changeReference = new JButton( "Set new level vector" );
//		horizontalLayoutPanel.add( changeReference );

//		final JButton defaultReference = new JButton( "Set default level vector" );
//		horizontalLayoutPanel.add( defaultReference );

//		changeReference.addActionListener( e -> {
//			targetNormalVector = BdvUtils.getCurrentViewNormalVector( bdv );
//			Utils.logVector( "New reference normal vector: ", targetNormalVector );
//		} );

//		defaultReference.addActionListener( e -> {
//			targetNormalVector = Arrays.copyOf( levelingVector, 3);
//			Utils.logVector( "New reference normal vector (default): ", levelingVector );
//		} );

		button.addActionListener( e -> BdvUtils.levelCurrentView( bdvHandle, targetNormalVector ) );

		return horizontalLayoutPanel;
	}

	public JPanel createBookmarksPanel( final BookmarkManager bookmarkManager )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();
		final JButton button = getButton( BUTTON_LABEL_VIEW );
		final Set< String > bookmarkNames = bookmarkManager.getBookmarkNames();
		JComboBox comboBox = new JComboBox<>( bookmarkNames.toArray( new String[bookmarkNames.size()] ) );
		setComboBoxDimensions( comboBox );
		button.addActionListener( e -> bookmarkManager.setView( ( String ) comboBox.getSelectedItem() ) );
		bookmarkManager.setBookmarkDropDown( comboBox );

		horizontalLayoutPanel.add( getJLabel( "bookmark" ) );
		horizontalLayoutPanel.add( comboBox );
		horizontalLayoutPanel.add( button );

		return horizontalLayoutPanel;
	}

	public JPanel createMoveToLocationPanel( BdvHandle bdv )
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

		return horizontalLayoutPanel;
	}

	public JPanel createInfoPanel( String projectLocation, String publicationURL )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton button = getButton( BUTTON_LABEL_HELP );

		final MoBIEInfo moBIEInfo = new MoBIEInfo( projectLocation, publicationURL );

		final JComboBox< String > comboBox = new JComboBox<>( moBIEInfo.getInfoChoices() );
		setComboBoxDimensions( comboBox );

		button.addActionListener( e -> {
			moBIEInfo.showInfo( ( String ) comboBox.getSelectedItem() );
		} );
		comboBox.setPrototypeDisplayValue( MoBIE.PROTOTYPE_DISPLAY_VALUE  );

		horizontalLayoutPanel.setSize( 0, 80 );
		final ImageIcon icon = createMobieIcon( 80 );
		final JLabel moBIE = new JLabel( "                   " );
		moBIE.setIcon( icon );

		horizontalLayoutPanel.add( moBIE );
		horizontalLayoutPanel.add( comboBox );
		horizontalLayoutPanel.add( button );

		return horizontalLayoutPanel;
	}

	public ImageIcon createMobieIcon( int size )
	{
		final URL resource = UserInterfacePanelsProvider.class.getResource( "/mobie.jpeg" );
		final ImageIcon imageIcon = new ImageIcon( resource );
		final Image scaledInstance = imageIcon.getImage().getScaledInstance( size, size, Image.SCALE_SMOOTH );
		return new ImageIcon( scaledInstance );
	}

	public JPanel createDatasetSelectionPanel( )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton button = getButton( BUTTON_LABEL_SWITCH );

		final String[] choices = datasets.stream().toArray( String[]::new );
		final JComboBox< String > comboBox = new JComboBox<>( choices );
		comboBox.setSelectedItem( moBIE.getDataset() );
		setComboBoxDimensions( comboBox );
		button.addActionListener( e -> switchDataset( ( String ) comboBox.getSelectedItem() ) );
		comboBox.setPrototypeDisplayValue( MoBIE.PROTOTYPE_DISPLAY_VALUE  );

		horizontalLayoutPanel.add( getJLabel( "dataset" ) );
		horizontalLayoutPanel.add( comboBox );
		horizontalLayoutPanel.add( button );

		return horizontalLayoutPanel;
	}

	// TODO: The project manager should do this!
	private void switchDataset( String dataset )
	{
		// TODO: make sure the Swing UI sources panel is fully visible before instantiating the new BDV
		moBIE.close();
		new MoBIE( moBIE.getProjectLocation(), moBIE.getOptions().dataset( dataset ) );
	}
}
