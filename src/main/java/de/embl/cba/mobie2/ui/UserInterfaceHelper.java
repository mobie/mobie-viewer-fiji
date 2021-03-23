package de.embl.cba.mobie2.ui;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.mobie.bdv.BdvViewChanger;
import de.embl.cba.mobie.bookmark.BookmarkManager;
import de.embl.cba.mobie.bookmark.Location;
import de.embl.cba.mobie.ui.MoBIE;
import de.embl.cba.mobie.ui.MoBIEInfo;
import de.embl.cba.mobie.ui.SourcesDisplayManager;
import de.embl.cba.mobie2.*;
import de.embl.cba.tables.SwingUtils;
import de.embl.cba.tables.color.ColorUtils;
import de.embl.cba.tables.image.SourceAndMetadata;
import ij.WindowManager;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.*;
import java.util.List;

import static de.embl.cba.mobie.utils.ui.SwingUtils.*;

public class UserInterfaceHelper
{
	public static final String BUTTON_LABEL_VIEW = "view";
	public static final String BUTTON_LABEL_MOVE = "move";
	public static final String BUTTON_LABEL_HELP = "show";
	public static final String BUTTON_LABEL_SWITCH = "switch";
	public static final String BUTTON_LABEL_LEVEL = "level";
	public static final String ADD = "add";

	private final MoBIE2 moBIE2;
	private int viewsSelectionPanelHeight;

	public UserInterfaceHelper( MoBIE2 moBIE2 )
	{
		this.moBIE2 = moBIE2;
	}

	public static JPanel createDisplaySettingsPanel()
	{
		final JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout(panel, BoxLayout.Y_AXIS ) );
		panel.setAlignmentX( Component.LEFT_ALIGNMENT );
		return panel;
	}

	public static void setLogWindowPositionAndSize( JFrame parentComponent )
	{
		final Frame log = WindowManager.getFrame( "Log" );
		if (log != null) {
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			final int logWindowHeight = screenSize.height - ( parentComponent.getLocationOnScreen().y + parentComponent.getHeight() + 20 );
			log.setSize( parentComponent.getWidth(), logWindowHeight  );
			log.setLocation( parentComponent.getLocationOnScreen().x, parentComponent.getLocationOnScreen().y + parentComponent.getHeight() );
		}
	}

	public static void setBdvWindowPositionAndSize( BdvHandle bdvHandle, JFrame frame )
	{
		BdvUtils.getViewerFrame( bdvHandle ).setLocation(
				frame.getLocationOnScreen().x + frame.getWidth(),
				frame.getLocationOnScreen().y );

		BdvUtils.getViewerFrame( bdvHandle ).setSize( frame.getHeight(), frame.getHeight() );
	}

	public JPanel createActionPanel()
	{
		final JPanel actionPanel = new JPanel();
		actionPanel.setLayout( new BoxLayout( actionPanel, BoxLayout.Y_AXIS ) );

		actionPanel.add( createInfoPanel( moBIE2.getProjectLocation(), moBIE2.getOptions().values.getPublicationURL() ) );
		actionPanel.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		actionPanel.add( createDatasetSelectionPanel() );
		actionPanel.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		actionPanel.add( createViewsSelectionPanel( moBIE2.getViews(), moBIE2.getViewer() ) );
		actionPanel.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		actionPanel.add( createMoveToLocationPanel( )  );

		if ( moBIE2.getLevelingVector() != null )
		{
			actionPanel.add( createLevelingPanel( moBIE2.getLevelingVector() ) );
		}

		return actionPanel;
	}

	public void addImageDisplaySettings( de.embl.cba.mobie2.ui.UserInterface userInterface, ImageDisplay imageDisplay )
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

		final JButton removeButton = createRemoveButton( userInterface, panel, imageDisplay, buttonDimensions );

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

		userInterface.addDisplaySettings( panel );
	}

	public JPanel createDisplaySettingsPanel( SegmentationDisplay segmentationDisplay )
	{
		// TODO: this is quite different because of the different coloring
		return null;
	}

	public JPanel createViewsSelectionPanel( List< View > views, Viewer viewer )
	{
		Map< String, List< View > > groupingsToViews = new HashMap<>(  );

		for ( View view : views )
		{
			final String group = view.menuItem.split( "/" )[ 0 ];
			if ( ! groupingsToViews.containsKey( group ) )
				groupingsToViews.put( group, new ArrayList<>( ));
			groupingsToViews.get( group ).add( view );
		}

		JPanel containerPanel = new JPanel( new BorderLayout() );
		containerPanel.setLayout( new BoxLayout( containerPanel, BoxLayout.Y_AXIS ) );

		for ( Map.Entry< String, List< View > > groupingToViews : groupingsToViews.entrySet() )
		{
			final JPanel selectionPanel = createSelectionPanel( viewer, groupingToViews.getKey(), groupingToViews.getValue() );
			containerPanel.add( selectionPanel );
		}

		viewsSelectionPanelHeight = groupingsToViews.keySet().size() * 40;

		return containerPanel;
	}

	public int getViewsSelectionPanelHeight()
	{
		return viewsSelectionPanelHeight;
	}

	private JPanel createSelectionPanel( Viewer viewer, String name, List< View > views )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final HashMap< String, View > nameToView = new HashMap<>();
		for ( View view : views )
		{
			nameToView.put( view.menuItem.split( "/" )[ 1 ], view );
		}

		final JComboBox< String > comboBox = new JComboBox<>( nameToView.keySet().toArray( new String[ 0 ] ) );

		final JButton button = getButton( ADD );
		button.addActionListener( e ->
		{
			SwingUtilities.invokeLater( () ->
			{
				final String viewName = ( String ) comboBox.getSelectedItem();
				final View view = nameToView.get( viewName );
				viewer.show( view );
			} );
		} );

		horizontalLayoutPanel.add( getJLabel( name ) );
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
		final URL resource = UserInterfaceHelper.class.getResource( "/mobie.jpeg" );
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
			final de.embl.cba.mobie2.ui.UserInterface userInterface,
			JPanel panel,
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

			userInterface.removeDisplaySettings( panel );
		} );

		return removeButton;
	}
}
