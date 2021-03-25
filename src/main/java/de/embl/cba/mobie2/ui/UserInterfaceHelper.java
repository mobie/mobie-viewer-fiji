package de.embl.cba.mobie2.ui;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.BdvHandle;
import bdv.util.BoundedValueDouble;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.BrightnessUpdateListener;
import de.embl.cba.mobie.bdv.BdvViewChanger;
import de.embl.cba.mobie.bookmark.BookmarkManager;
import de.embl.cba.mobie.bookmark.Location;
import de.embl.cba.mobie.ui.MoBIE;
import de.embl.cba.mobie.ui.MoBIEInfo;
import de.embl.cba.mobie.ui.SourcesDisplayManager;
import de.embl.cba.mobie2.*;
import de.embl.cba.mobie2.display.ImageDisplay;
import de.embl.cba.mobie2.display.SegmentationDisplay;
import de.embl.cba.mobie2.display.SourceDisplay;
import de.embl.cba.mobie2.view.View;
import de.embl.cba.mobie2.view.Viewer;
import de.embl.cba.mobie2.view.ViewerHelper;
import de.embl.cba.tables.SwingUtils;
import de.embl.cba.tables.color.ColorUtils;
import de.embl.cba.tables.image.SourceAndMetadata;
import ij.WindowManager;
import net.imglib2.type.numeric.RealType;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;
import sc.fiji.bdvpg.sourceandconverter.display.ConverterChanger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static de.embl.cba.mobie.utils.ui.SwingUtils.*;

public class UserInterfaceHelper
{
	private static final int[] BUTTON_DIMENSIONS = new int[]{ 50, 30 };
	public static final Dimension PREFERRED_BUTTON_SIZE = new Dimension( BUTTON_DIMENSIONS[ 0 ], BUTTON_DIMENSIONS[ 1 ] );
	private static final String VIEW = "view";
	private static final String MOVE = "move";
	private static final String HELP = "show";
	private static final String SWITCH = "switch";
	private static final String LEVEL = "level";
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

	public static void showBrightnessDialog(
			String name,
			List< ConverterSetup > converterSetups,
			double rangeMin,
			double rangeMax )
	{
		JFrame frame = new JFrame( name );
		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

		final double currentRangeMin = converterSetups.get( 0 ).getDisplayRangeMin();
		final double currentRangeMax = converterSetups.get( 0 ).getDisplayRangeMax();

		final BoundedValueDouble min =
				new BoundedValueDouble(
						rangeMin,
						rangeMax,
						currentRangeMin );

		final BoundedValueDouble max =
				new BoundedValueDouble(
						rangeMin,
						rangeMax,
						currentRangeMax );

		double spinnerStepSize = ( currentRangeMax - currentRangeMin ) / 100.0;

		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.PAGE_AXIS ) );
		final SliderPanelDouble minSlider =
				new SliderPanelDouble( "Min", min, spinnerStepSize );
		minSlider.setNumColummns( 7 );
		minSlider.setDecimalFormat( "####E0" );

		final SliderPanelDouble maxSlider =
				new SliderPanelDouble( "Max", max, spinnerStepSize );
		maxSlider.setNumColummns( 7 );
		maxSlider.setDecimalFormat( "####E0" );

		final BrightnessUpdateListener brightnessUpdateListener =
				new BrightnessUpdateListener(
						min, max, minSlider, maxSlider, converterSetups );

		min.setUpdateListener( brightnessUpdateListener );
		max.setUpdateListener( brightnessUpdateListener );

		panel.add( minSlider );
		panel.add( maxSlider );

		frame.setContentPane( panel );

		//Display the window.
		frame.setBounds( MouseInfo.getPointerInfo().getLocation().x,
				MouseInfo.getPointerInfo().getLocation().y,
				120, 10);
		frame.setResizable( false );
		frame.pack();
		frame.setVisible( true );

	}

	public JPanel createActionPanel()
	{
		final JPanel actionPanel = new JPanel();
		actionPanel.setLayout( new BoxLayout( actionPanel, BoxLayout.Y_AXIS ) );

		actionPanel.add( createInfoPanel( moBIE2.getProjectLocation(), moBIE2.getOptions().values.getPublicationURL() ) );
		actionPanel.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		// actionPanel.add( createDatasetSelectionPanel() );
		actionPanel.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		actionPanel.add( createViewsSelectionPanel( ) );
		actionPanel.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		actionPanel.add( createMoveToLocationPanel( )  );

		if ( moBIE2.getLevelingVector() != null )
		{
			actionPanel.add( createLevelingPanel( moBIE2.getLevelingVector() ) );
		}

		return actionPanel;
	}

	public void addImageDisplaySettings( UserInterface userInterface, ImageDisplay display )
	{
		JPanel panel = createDisplayPanel( display.name );

		setPanelColor( panel, display.color );

		JButton colorButton = createColorButton( display, panel );

		final JButton brightnessButton = createImageDisplayBrightnessButton( display );

		final JButton removeButton = createRemoveButton( userInterface, panel, display );

		final JCheckBox bigDataViewerVisibilityCheckbox = createImageViewerVisibilityCheckbox( display, true );

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
		for ( SourceAndConverter< ? > sourceAndConverter : display.sourceAndConverters )
		{
			SourceAndConverterServices.getSourceAndConverterDisplayService().getConverterSetup( sourceAndConverter ).setupChangeListeners().add( setup -> {
				// color changed listener
				setPanelColor( panel, setup.getColor().toString());
			} );
		}

		userInterface.addDisplaySettings( panel );
	}

	private JPanel createDisplayPanel( String name )
	{
		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.LINE_AXIS ) );
		panel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 0, 10 ) );
		panel.add( Box.createHorizontalGlue() );
		JLabel label = new JLabel(name );
		label.setHorizontalAlignment( SwingUtilities.CENTER );
		panel.add( label );

		return panel;
	}

	public void addSegmentationDisplaySettings( UserInterface userInterface, SegmentationDisplay display )
	{
		JPanel panel = createDisplayPanel( display.name );

		// TODO: make use of alpha
//		final JButton brightnessButton = createImageDisplayBrightnessButton( display, BUTTON_DIMENSIONS );

		// TODO:
//		final JCheckBox volumeVisibilityCheckbox =
//				createVolumeViewVisibilityCheckbox(
//						displayManager,
//						viewSelectionDimensions,
//						sourceAndMetadataList.get( 0 ),
//						sourceAndMetadataList.get( 0 ).metadata().showImageIn3d || sourceAndMetadataList.get( 0 ).metadata().showSelectedSegmentsIn3d );

		//panel.add( brightnessButton );
		panel.add( createRemoveButton( userInterface, panel, display ) );
		//panel.add( volumeVisibilityCheckbox );
		panel.add( createImageViewerVisibilityCheckbox( display, true ) );
		panel.add( createTableViewerVisibilityCheckbox( display, true ) );
		panel.add( createScatterPlotViewerVisibilityCheckbox( display, true ) );

		userInterface.addDisplaySettings( panel );
	}

	public JPanel createViewsSelectionPanel( )
	{
		final HashMap< String, View > views = moBIE2.getViews();

		Map< String, Map< String, View > > groupingsToViews = new HashMap<>(  );

		for ( String viewName : views.keySet() )
		{
			final View view = views.get( viewName );
			if ( ! groupingsToViews.containsKey(  view.uiSelectionGroup ) )
				groupingsToViews.put( view.uiSelectionGroup, new HashMap<>( ));
			groupingsToViews.get( view.uiSelectionGroup ).put( viewName, view );
		}

		JPanel containerPanel = new JPanel( new BorderLayout() );
		containerPanel.setLayout( new BoxLayout( containerPanel, BoxLayout.Y_AXIS ) );

		for ( String grouping : groupingsToViews.keySet() )
		{
			final JPanel selectionPanel = createSelectionPanel( moBIE2, grouping, groupingsToViews.get( grouping ) );
			containerPanel.add( selectionPanel );
		}

		viewsSelectionPanelHeight = groupingsToViews.keySet().size() * 40;

		return containerPanel;
	}

	public int getViewsSelectionPanelHeight()
	{
		return viewsSelectionPanelHeight;
	}

	private JPanel createSelectionPanel( MoBIE2 moBIE2, String panelName, Map< String, View > views )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JComboBox< String > comboBox = new JComboBox<>( views.keySet().toArray( new String[ 0 ] ) );

		final JButton button = getButton( ADD );
		button.addActionListener( e ->
		{
			SwingUtilities.invokeLater( () ->
			{
				final String viewName = ( String ) comboBox.getSelectedItem();
				final View view = views.get( viewName );
				moBIE2.getViewer().show( view );
			} );
		} );

		horizontalLayoutPanel.add( getJLabel( panelName ) );
		horizontalLayoutPanel.add( comboBox );
		horizontalLayoutPanel.add( button );

		return horizontalLayoutPanel;
	}

	public JPanel createLevelingPanel( double[] levelingVector )
	{
		final double[] targetNormalVector = Arrays.copyOf( levelingVector, 3 );

		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton button = getButton( LEVEL );
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

		button.addActionListener( e -> BdvUtils.levelCurrentView( moBIE2.getViewer().getImageViewer().getBdvHandle(), targetNormalVector ) );

		return horizontalLayoutPanel;
	}

	public JPanel createBookmarksPanel( final BookmarkManager bookmarkManager )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();
		final JButton button = getButton( VIEW );
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

		final JButton button = getButton( MOVE );

		final JTextField jTextField = new JTextField( "120.5,115.3,201.5" );
		jTextField.setPreferredSize( new Dimension( COMBOBOX_WIDTH - 3, TEXT_FIELD_HEIGHT ) );
		jTextField.setMaximumSize( new Dimension( COMBOBOX_WIDTH - 3, TEXT_FIELD_HEIGHT ) );
		button.addActionListener( e -> BdvViewChanger.moveToLocation( moBIE2.getViewer().getImageViewer().getBdvHandle(), new Location( jTextField.getText() ) ) );

		horizontalLayoutPanel.add( getJLabel( "location" ) );
		horizontalLayoutPanel.add( jTextField );
		horizontalLayoutPanel.add( button );

		return horizontalLayoutPanel;
	}

	public JPanel createInfoPanel( String projectLocation, String publicationURL )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton button = getButton( HELP );

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

	private static JCheckBox createImageViewerVisibilityCheckbox(
			SourceDisplay sourceDisplay,
			boolean isVisible )
	{
		JCheckBox checkBox = new JCheckBox( "S" );
		checkBox.setSelected( isVisible );
		checkBox.setPreferredSize( PREFERRED_BUTTON_SIZE );

		checkBox.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				for ( SourceAndConverter< ? > sourceAndConverter : sourceDisplay.sourceAndConverters )
				{
					// TODO: replace by new API: isVisible
					if ( checkBox.isSelected() )
						SourceAndConverterServices.getSourceAndConverterDisplayService().makeVisible( sourceAndConverter );
					else
						SourceAndConverterServices.getSourceAndConverterDisplayService().makeInvisible( sourceAndConverter );
				}
			}
		} );

		return checkBox;
	}

	private static JCheckBox createTableViewerVisibilityCheckbox(
			SegmentationDisplay sourceDisplay,
			boolean isVisible )
	{
		JCheckBox checkBox = new JCheckBox( "T" );
		checkBox.setSelected( isVisible );
		checkBox.setPreferredSize( PREFERRED_BUTTON_SIZE );
		checkBox.addActionListener( e -> SwingUtilities.invokeLater( () -> sourceDisplay.tableViewer.getFrame().setVisible( checkBox.isSelected() ) ) );

		sourceDisplay.tableViewer.getFrame().addWindowListener(
				new WindowAdapter() {
					public void windowClosing( WindowEvent ev) {
						checkBox.setSelected( false );
					}
		});

		return checkBox;
	}

	private static JCheckBox createScatterPlotViewerVisibilityCheckbox(
			SegmentationDisplay display,
			boolean isVisible )
	{
		final AtomicBoolean recreate = new AtomicBoolean( false );

		JCheckBox checkBox = new JCheckBox( "P" );
		checkBox.setSelected( isVisible );
		checkBox.setPreferredSize( PREFERRED_BUTTON_SIZE );
		checkBox.addActionListener( e ->
			SwingUtilities.invokeLater( () ->
				{
					if ( recreate.get() )
					{
						ViewerHelper.showInScatterPlotViewer( display );
						recreate.set( false );
					}
					else
					{
						display.scatterPlotViewer.getWindow().setVisible( checkBox.isSelected() );
					}
				} ) );

		display.scatterPlotViewer.getWindow().addWindowListener(
				new WindowAdapter() {
					public void windowClosing( WindowEvent ev) {
						SwingUtilities.invokeLater( () ->
						{
							checkBox.setSelected( false );

							// The scatterPlot BDV Window has been closed.
							// Simply setting it visible again does not work,
							// but leads to an empty window.
							// Probably because BDV itself is listening to the
							// window closing and releases some resources?!
							// Thus we need to recreate it from scratch.
							recreate.set( true );
						} );
					}
				});

		return checkBox;
	}

	// TODO: Not clear what we want here... close the whole window
	//   or remove the segments? Probably remove segments, because it can be
	//   interesting to see segments from different segmentation sources
	//   together in the same volume rendering
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
					// TODO: Old code, will not work
					sam.metadata().showImageIn3d = checkBox.isSelected();
					sam.metadata().showSelectedSegmentsIn3d = checkBox.isSelected();
					sourcesDisplayManager.updateSegments3dView( sam, sourcesDisplayManager );
					sourcesDisplayManager.updateSource3dView( sam, sourcesDisplayManager, false );
				}).start();
			}
		} );

		return checkBox;
	}

	public static JButton createImageDisplayBrightnessButton( ImageDisplay imageDisplay )
	{
		JButton button = new JButton( "B" );
		button.setPreferredSize( PREFERRED_BUTTON_SIZE );

		button.addActionListener( e ->
		{
			final ArrayList< ConverterSetup > converterSetups = new ArrayList<>();
			for ( SourceAndConverter< ? > sourceAndConverter : imageDisplay.sourceAndConverters )
			{
				converterSetups.add( SourceAndConverterServices.getSourceAndConverterDisplayService().getConverterSetup( sourceAndConverter ) );
			}

			UserInterfaceHelper.showBrightnessDialog(
					imageDisplay.name,
					converterSetups,
					0,   // TODO: determine somehow...
					65535 );
		} );

		return button;
	}

	private static JButton createColorButton( ImageDisplay imageDisplay, JPanel parentPanel )
	{
		JButton colorButton = new JButton( "C" );

		colorButton.setPreferredSize( PREFERRED_BUTTON_SIZE);

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
			SourceDisplay sourceDisplay )
	{
		JButton removeButton = new JButton( "X" );
		removeButton.setPreferredSize( PREFERRED_BUTTON_SIZE );

		removeButton.addActionListener( e ->
		{
			for ( SourceAndConverter< ? > sourceAndConverter : sourceDisplay.sourceAndConverters )
			{
				final Set< BdvHandle > bdvHandles = SourceAndConverterServices.getSourceAndConverterDisplayService().getDisplaysOf( sourceAndConverter );
				SourceAndConverterServices.getSourceAndConverterDisplayService().removeFromAllBdvs( sourceAndConverter );
			}

			userInterface.removeDisplaySettings( panel );
		} );

		return removeButton;
	}
}
