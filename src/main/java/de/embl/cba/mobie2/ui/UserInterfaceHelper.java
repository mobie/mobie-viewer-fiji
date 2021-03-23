package de.embl.cba.mobie2.ui;

import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.mobie.bdv.BdvViewChanger;
import de.embl.cba.mobie.bookmark.BookmarkManager;
import de.embl.cba.mobie.bookmark.Location;
import de.embl.cba.mobie.ui.MoBIE;
import de.embl.cba.mobie.ui.MoBIEInfo;
import de.embl.cba.mobie.ui.SourcesDisplayManager;
import de.embl.cba.mobie.ui.UserInterface;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.mobie2.ImageDisplay;
import de.embl.cba.mobie2.MoBIE2;
import de.embl.cba.mobie2.SegmentationDisplay;
import de.embl.cba.mobie2.SourceDisplay;
import de.embl.cba.tables.SwingUtils;
import de.embl.cba.tables.color.ColorUtils;
import de.embl.cba.tables.image.SourceAndMetadata;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.*;

import static de.embl.cba.mobie.utils.ui.SwingUtils.*;

public class UIComponentsProvider
{
	public static final String BUTTON_LABEL_VIEW = "view";
	public static final String BUTTON_LABEL_MOVE = "move";
	public static final String BUTTON_LABEL_HELP = "show";
	public static final String BUTTON_LABEL_SWITCH = "switch";
	public static final String BUTTON_LABEL_LEVEL = "level";
	public static final String BUTTON_LABEL_ADD = "add";

	private final ArrayList< String > datasets;
	private final MoBIE2 moBIE2;
	private ArrayList< String > sortedModalities;
	private int sourceSelectionPanelHeight;

	public UIComponentsProvider( MoBIE2 moBIE2 )
	{
		this.moBIE2 = moBIE2;
		this.displayManager = moBIE2.getVi();
		this.datasets = moBIE2.getDatasets();
	}

	static JPanel createDisplaySettingsPanel()
	{
		final JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout(panel, BoxLayout.Y_AXIS ) );
		panel.setAlignmentX( Component.LEFT_ALIGNMENT );
		return panel;
	}

	public JPanel createActionPanel()
	{
		final JPanel actionPanel = new JPanel();
		actionPanel.setLayout( new BoxLayout( actionPanel, BoxLayout.Y_AXIS ) );

		actionPanel.add( createInfoPanel( moBIE2.getProjectLocation(), moBIE2.getOptions().values.getPublicationURL() ) );
		actionPanel.add( new JSeparator( SwingConstants.HORIZONTAL ) );

		actionPanel.add( createDatasetSelectionPanel() );
		actionPanel.add( new JSeparator( SwingConstants.HORIZONTAL ) );

		actionPanel.add( createSourceSelectionPanel( moBIE2.getSourcesDisplayManager() ) );
		actionPanel.add( new JSeparator( SwingConstants.HORIZONTAL ) );

		actionPanel.add( createBookmarksPanel( moBIE2.getBookmarkManager() )  );
		actionPanel.add( createMoveToLocationPanel( )  );

		if ( moBIE2.getLevelingVector() != null )
		{
			actionPanel.add( createLevelingPanel( moBIE2.getLevelingVector() ) );
		}
		return actionPanel;
	}

	public JPanel createImageDisplaySettingsPanel( ImageDisplay imageDisplay )
	{
		JPanel panel = new JPanel();

		panel.setLayout( new BoxLayout(panel, BoxLayout.LINE_AXIS) );
		panel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 0, 10 ) );
		panel.add( Box.createHorizontalGlue() );

		setPanelColor( panel, imageDisplay.color );

		JLabel sourceNameLabel = new JLabel( imageDisplay.name );
		sourceNameLabel.setHorizontalAlignment( SwingUtilities.CENTER );
		panel.add( sourceNameLabel );

		int[] buttonDimensions = new int[]{ 50, 30 };
		int[] viewSelectionDimensions = new int[]{ 50, 30 };

		JButton colorButton = createColorButton( imageDisplay, panel, buttonDimensions );

		final JButton brightnessButton = createBrightnessButton( imageDisplay, buttonDimensions );

		final JButton removeButton = createRemoveButton( imageDisplay, buttonDimensions );

		final JCheckBox bigDataViewerVisibilityCheckbox = createVisibilityCheckbox( imageDisplay, buttonDimensions, true );

		// TODO: Can we adapt this for source groups?
//		final JCheckBox volumeVisibilityCheckbox =
//				createVolumeViewVisibilityCheckbox(
//						displayManager,
//						viewSelectionDimensions,
//						sourceAndMetadataList.get( 0 ),
//						sourceAndMetadataList.get( 0 ).metadata().showImageIn3d || sourceAndMetadataList.get( 0 ).metadata().showSelectedSegmentsIn3d );

		panel.add( colorButton );
		panel.add( brightnessButton );
		panel.add( removeButton );
		//panel.add( volumeVisibilityCheckbox );
		panel.add( bigDataViewerVisibilityCheckbox );

		// make the panel color listen to color changes of the sources
		for ( SourceAndConverter< ? > sourceAndConverter : imageDisplay.sourceAndConverters )
		{
			SourceAndConverterServices.getSourceAndConverterDisplayService().getConverterSetup( sourceAndConverter ).setupChangeListeners().add( setup -> {
				// color changed listener
				setPanelColor( panel, setup.getColor().toString());
			} );
		}
		return panel;
	}

	public JPanel createDisplaySettingsPanel( SegmentationDisplay segmentationDisplay )
	{
		// TODO: this is quite different because of the different coloring
		return null;
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
			final JPanel selectionComboBoxAndButtonPanel = createSourceSelectionComboBoxAndButtonPanel( selectionNameAndModalityToSourceName, comboBox, modality );
			sourcesSelectionPanel.add( selectionComboBoxAndButtonPanel );
		}

		sourceSelectionPanelHeight = sortedModalities.size() * 40;

		return sourcesSelectionPanel;
	}

	public int getSourceSelectionPanelHeight()
	{
		return sourceSelectionPanelHeight;
	}

	private JPanel createSourceSelectionComboBoxAndButtonPanel(
			HashMap< String, String > selectionNameAndModalityToSourceName,
			final JComboBox comboBox,
			final String modality )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		if ( comboBox.getModel().getSize() == 0 ) return horizontalLayoutPanel;

		final JButton button = getButton( BUTTON_LABEL_ADD );
		button.addActionListener( e ->
		{
			SwingUtilities.invokeLater( () ->
			{
				final String selectedSource = ( String ) comboBox.getSelectedItem();
				final String sourceName = selectionNameAndModalityToSourceName.get( selectedSource + "-" + modality );

				displayManager.show( sourceName );
			} );
		} );

		final JLabel comp = getJLabel( modality );

		horizontalLayoutPanel.add( comp );
		horizontalLayoutPanel.add( comboBox );
		horizontalLayoutPanel.add( button );

		return horizontalLayoutPanel;
	}

	public JPanel createLevelingPanel( double[] levelingVector )
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

		button.addActionListener( e -> BdvUtils.levelCurrentView( displayManager.getBdv(), targetNormalVector ) );

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

	public JPanel createMoveToLocationPanel( )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton button = getButton( BUTTON_LABEL_MOVE );

		final JTextField jTextField = new JTextField( "120.5,115.3,201.5" );
		jTextField.setPreferredSize( new Dimension( COMBOBOX_WIDTH - 3, TEXT_FIELD_HEIGHT ) );
		jTextField.setMaximumSize( new Dimension( COMBOBOX_WIDTH - 3, TEXT_FIELD_HEIGHT ) );
		button.addActionListener( e -> BdvViewChanger.moveToLocation( displayManager.getBdv(), new Location( jTextField.getText() ) ) );

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
		final URL resource = UIComponentsProvider.class.getResource( "/mobie.jpeg" );
		final ImageIcon imageIcon = new ImageIcon( resource );
		final Image scaledInstance = imageIcon.getImage().getScaledInstance( size, size, Image.SCALE_SMOOTH );
		return new ImageIcon( scaledInstance );
	}

	public JPanel createDatasetSelectionPanel( )
	{
//		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();
//
//		final JButton button = getButton( BUTTON_LABEL_SWITCH );
//
//		final String[] choices = datasets.stream().toArray( String[]::new );
//		final JComboBox< String > comboBox = new JComboBox<>( choices );
//		comboBox.setSelectedItem( moBIE2.getDataset() );
//		setComboBoxDimensions( comboBox );
//		button.addActionListener( e -> switchDataset( ( String ) comboBox.getSelectedItem() ) );
//		comboBox.setPrototypeDisplayValue( MoBIE.PROTOTYPE_DISPLAY_VALUE  );
//
//		horizontalLayoutPanel.add( getJLabel( "dataset" ) );
//		horizontalLayoutPanel.add( comboBox );
//		horizontalLayoutPanel.add( button );
//
//		return horizontalLayoutPanel;
		throw new RuntimeException(  );
	}

	// TODO: The project manager should do this!
	private void switchDataset( String dataset )
	{
		// TODO: make sure the Swing UI sources panel is fully visible before instantiating the new BDV
		moBIE2.close();
		new MoBIE( moBIE2.getProjectLocation(), moBIE2.getOptions().dataset( dataset ) );
	}

	public static JCheckBox createVisibilityCheckbox(
			SourceDisplay sourceDisplay,
			int[] buttonDimensions,
			boolean isVisible )
	{
		JCheckBox checkBox = new JCheckBox( "S" );
		checkBox.setSelected( isVisible );
		checkBox.setPreferredSize( new Dimension( buttonDimensions[ 0 ], buttonDimensions[ 1 ] ) );

		checkBox.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				for ( SourceAndConverter< ? > sourceAndConverter : sourceDisplay.sourceAndConverters )
				{
					if ( checkBox.isSelected() )
						SourceAndConverterServices.getSourceAndConverterDisplayService().makeVisible( sourceAndConverter );
					else
						SourceAndConverterServices.getSourceAndConverterDisplayService().makeInvisible( sourceAndConverter );
				}
			}
		} );

		return checkBox;
	}

	public static JCheckBox createVolumeViewVisibilityCheckbox(
			SourcesDisplayManager sourcesDisplayManager,
			int[] dims,
			SourceAndMetadata< ? > sam,
			boolean isVisible )
	{
		JCheckBox checkBox = new JCheckBox( "V" );
		checkBox.setSelected( isVisible );
		checkBox.setPreferredSize( new Dimension( dims[ 0 ], dims[ 1 ] ) );

		checkBox.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				new Thread( () -> {
					sam.metadata().showImageIn3d = checkBox.isSelected();
					sam.metadata().showSelectedSegmentsIn3d = checkBox.isSelected();
					sourcesDisplayManager.updateSegments3dView( sam, sourcesDisplayManager );
					sourcesDisplayManager.updateSource3dView( sam, sourcesDisplayManager, false );
				}).start();
			}
		} );

		return checkBox;
	}

	public static JButton createBrightnessButton( ImageDisplay imageDisplay, int[] buttonDimensions )
	{
		JButton button = new JButton( "B" );
		button.setPreferredSize( new Dimension(
				buttonDimensions[ 0 ],
				buttonDimensions[ 1 ] ) );

		button.addActionListener( e ->
		{
			final ArrayList< ConverterSetup > converterSetups = new ArrayList<>();
			for ( SourceAndConverter< ? > sourceAndConverter : imageDisplay.sourceAndConverters )
			{
				converterSetups.add( SourceAndConverterServices.getSourceAndConverterDisplayService().getConverterSetup( sourceAndConverter ) );
			}

			UserInterface.showBrightnessDialog(
					imageDisplay.name,
					converterSetups,
					0,   // TODO: determine somehow...
					65535 );
		} );

		return button;
	}

	private static JButton createColorButton( ImageDisplay imageDisplay, JPanel parentPanel, int[] buttonDimensions )
	{
		JButton colorButton = new JButton( "C" );

		colorButton.setPreferredSize( new Dimension( buttonDimensions[ 0 ], buttonDimensions[ 1 ] ) );

		colorButton.addActionListener( e ->
		{
			Color color = JColorChooser.showDialog( null, "", null );
			if ( color == null ) return;

			parentPanel.setBackground( color );

			for ( SourceAndConverter< ? > sourceAndConverter : imageDisplay.sourceAndConverters )
			{
				new ColorChanger( sourceAndConverter, ColorUtils.getARGBType( color ) ).run();
			}
		} );

		return colorButton;
	}

	private void setPanelColor( JPanel panel, String colorString )
	{
		final Color color = ColorUtils.getColor( colorString );
		if ( color != null )
		{
			panel.setOpaque( true );
			panel.setBackground( color );
		}
	}

	// TODO: this should also close the table a.s.o. if it is a segmentation source
	private static JButton createRemoveButton(
			SourceDisplay sourceDisplay,
			int[] buttonDimensions )
	{
		JButton removeButton = new JButton( "X" );
		removeButton.setPreferredSize( new Dimension( buttonDimensions[ 0 ], buttonDimensions[ 1 ] ) );

		removeButton.addActionListener( e ->
		{
			for ( SourceAndConverter< ? > sourceAndConverter : sourceDisplay.sourceAndConverters )
			{
				SourceAndConverterServices.getSourceAndConverterDisplayService().removeFromAllBdvs( sourceAndConverter );
			}
		} );

		return removeButton;
	}
}
