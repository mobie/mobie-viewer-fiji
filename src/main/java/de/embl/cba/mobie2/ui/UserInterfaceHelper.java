package de.embl.cba.mobie2.ui;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.BdvHandle;
import bdv.util.BoundedValueDouble;
import bdv.viewer.SourceAndConverter;
import com.formdev.flatlaf.FlatLightLaf;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.BrightnessUpdateListener;
import de.embl.cba.mobie2.bdv.BdvViewChanger;
import de.embl.cba.mobie.bookmark.BookmarkManager;
import de.embl.cba.mobie.bookmark.Location;
import de.embl.cba.mobie.ui.MoBIE;
import de.embl.cba.mobie.ui.MoBIEInfo;
import de.embl.cba.mobie2.*;
import de.embl.cba.mobie2.color.OpacityAdjuster;
import de.embl.cba.mobie2.display.ImageDisplay;
import de.embl.cba.mobie2.display.SegmentationDisplay;
import de.embl.cba.mobie2.display.Display;
import de.embl.cba.mobie2.grid.GridView;
import de.embl.cba.mobie2.view.ViewerManager;
import de.embl.cba.mobie2.view.View;
import de.embl.cba.tables.SwingUtils;
import de.embl.cba.tables.color.ColorUtils;
import ij.WindowManager;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformChanger;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;

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

import static de.embl.cba.mobie2.ui.SwingHelper.*;

public class UserInterfaceHelper
{
	private static final Dimension PREFERRED_BUTTON_SIZE = new Dimension( 30, 30 );
	private static final Dimension PREFERRED_CHECKBOX_SIZE = new Dimension( 40, 30 );
	private static final Dimension PREFERRED_SPACE_SIZE = new Dimension( 10, 30 );
	private static final String VIEW = "view";
	private static final String MOVE = "move";
	private static final String HELP = "show";
	private static final String SWITCH = "switch";
	private static final String LEVEL = "level";
	private static final String ADD = "view";
	public static final int SPACING = 20;

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

	public static void setLogWindowPositionAndSize( Window reference )
	{
		final Frame log = WindowManager.getFrame( "Log" );
		if (log != null) {
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			final int logWindowHeight = screenSize.height - ( reference.getLocationOnScreen().y + reference.getHeight() + 2 * SPACING );
			log.setSize( reference.getWidth(), logWindowHeight  );
			log.setLocation( reference.getLocationOnScreen().x, reference.getLocationOnScreen().y + SPACING + reference.getHeight() );
		}
	}

	public static void rightAlignWindow( Window reference, Window window, boolean adjustWidth, boolean adjustHeight )
	{
		window.setLocation(
				reference.getLocationOnScreen().x + reference.getWidth() + SPACING,
				reference.getLocationOnScreen().y );

		if ( adjustWidth )
			window.setSize( reference.getWidth(), window.getHeight() );

		if ( adjustHeight )
			window.setSize( window.getWidth(), reference.getHeight() );
	}

	public static void bottomAlignWindow( Window reference, Window window )
	{
		window.setLocation(
				reference.getLocationOnScreen().x,
				reference.getLocationOnScreen().y + reference.getHeight() + SPACING
		);

		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		window.setPreferredSize( new Dimension(
				reference.getWidth(),
				( screenSize.height - SPACING ) - ( reference.getHeight() + reference.getLocationOnScreen().y ) )  );
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

		final BrightnessUpdateListener brightnessUpdateListener = new BrightnessUpdateListener( min, max, minSlider, maxSlider, converterSetups );

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

	public static void showOpacityDialog(
			String name,
			List< SourceAndConverter< ? > > sourceAndConverters,
			BdvHandle bdvHandle )
	{
		JFrame frame = new JFrame( name );
		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

		// TODO: This cast requires that the sourceAndConverter implements
		//   an OpacityAdjuster; how to do this more cleanly?
		final double current = ( (OpacityAdjuster) sourceAndConverters.get( 0 ).getConverter()).getOpacity();

		final BoundedValueDouble selection =
				new BoundedValueDouble(
						0.0,
						1.0,
						current );

		double spinnerStepSize = 0.05;

		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.PAGE_AXIS ) );
		final SliderPanelDouble slider = new SliderPanelDouble( "Opacity", selection, spinnerStepSize );
		slider.setNumColummns( 3 );
		slider.setDecimalFormat( "#.##" );

		final OpacityUpdateListener opacityUpdateListener =
				new OpacityUpdateListener( selection, slider, sourceAndConverters, bdvHandle );

		selection.setUpdateListener( opacityUpdateListener );
		panel.add( slider );

		frame.setContentPane( panel );

		//Display the window.
		frame.setBounds( MouseInfo.getPointerInfo().getLocation().x,
				MouseInfo.getPointerInfo().getLocation().y,
				120, 10);
		frame.setResizable( false );
		frame.pack();
		frame.setVisible( true );

	}

	public static int getDefaultWindowWidth()
	{
		return (int) ( Toolkit.getDefaultToolkit().getScreenSize().width / 3.1 );
	}

	public static void setLafSwingLookAndFeel() {
		FlatLightLaf.install();
		try {
			UIManager.setLookAndFeel( new FlatLightLaf() );
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void setSystemSwingLookAndFeel() {
		try {
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public JPanel createGridViewDisplaySettingsPanel( GridView gridView )
	{
		JPanel panel = createDisplayPanel( gridView.getName() );

		// Buttons
		panel.add( createSpace() );
		panel.add( createButtonPlaceholder() );
		panel.add( createOpacityButton( Arrays.asList( gridView.getSourceAndConverter() ), gridView.getName(), gridView.getBdvHandle() ) );
		panel.add( createButtonPlaceholder() );
		panel.add( createButtonPlaceholder() );
		panel.add( createButtonPlaceholder() ); //panel.add( createRemoveButton( display ) );
		// Checkboxes
		panel.add( createSpace() );
		panel.add( createSliceViewerVisibilityCheckbox( true, Arrays.asList( gridView.getSourceAndConverter() ) ) );
		panel.add( createCheckboxPlaceholder() );
		panel.add( createWindowVisibilityCheckbox( true, gridView.getTableViewer().getWindow() ) );
		panel.add( createCheckboxPlaceholder() ); //panel.add( createScatterPlotViewerVisibilityCheckbox( display, true ) );
		return panel;
	}

	public static class OpacityUpdateListener implements BoundedValueDouble.UpdateListener
	{
		final private List< SourceAndConverter< ? > > sourceAndConverters;
		private final BdvHandle bdvHandle;
		final private BoundedValueDouble value;
		private final SliderPanelDouble slider;

		public OpacityUpdateListener( BoundedValueDouble value,
									  SliderPanelDouble slider,
									  List< SourceAndConverter< ? > > sourceAndConverters, BdvHandle bdvHandle )
		{
			this.value = value;
			this.slider = slider;
			this.sourceAndConverters = sourceAndConverters;
			this.bdvHandle = bdvHandle;
		}

		@Override
		public void update()
		{
			slider.update();

			for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
			{
				final double currentValue = value.getCurrentValue();

				( ( OpacityAdjuster ) sourceAndConverter.getConverter() ).setOpacity( currentValue );
				if ( sourceAndConverter.asVolatile() != null )
					( ( OpacityAdjuster ) sourceAndConverter.asVolatile().getConverter() ).setOpacity( currentValue );
			}

			bdvHandle.getViewerPanel().requestRepaint();
		}
	}

	public JPanel createActionPanel()
	{
		final JPanel actionPanel = new JPanel();
		actionPanel.setLayout( new BoxLayout( actionPanel, BoxLayout.Y_AXIS ) );

		actionPanel.add( createInfoPanel( moBIE2.getProjectLocation(), moBIE2.getOptions().values.getPublicationURL() ) );
		// actionPanel.add( new JSeparator( SwingConstants.HORIZONTAL ) );
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

	public JPanel createImageDisplaySettingsPanel( ImageDisplay display )
	{
		JPanel panel = createDisplayPanel( display.getName() );

		setPanelColor( panel, display.getColor() );

		// Buttons
		panel.add( createSpace() );
		panel.add( createFocusButton( display, display.sourceAndConverters, display.sliceViewer.getBdvHandle() ) );
		panel.add( createOpacityButton( display.sourceAndConverters, display.getName(), display.sliceViewer.getBdvHandle() ) );
		panel.add( createColorButton( panel, display.sourceAndConverters ) );
		panel.add( createImageDisplayBrightnessButton( display ) );
		panel.add( createRemoveButton( display ) );
		// Checkboxes
		panel.add( createSpace() );
		panel.add( createSliceViewerVisibilityCheckbox( true, display.sourceAndConverters ) );
		panel.add( createCheckboxPlaceholder() ); // TODO: createVolume...
		panel.add( createCheckboxPlaceholder() );
		panel.add( createCheckboxPlaceholder() );


		// make the panel color listen to color changes of the sources
		for ( SourceAndConverter< ? > sourceAndConverter : display.sourceAndConverters )
		{
			SourceAndConverterServices.getSourceAndConverterDisplayService().getConverterSetup( sourceAndConverter ).setupChangeListeners().add( setup -> {
				// color changed listener
				setPanelColor( panel, setup.getColor().toString());
			} );
		}

		return panel;
	}

	private JPanel createDisplayPanel( String name )
	{
		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.LINE_AXIS ) );
		panel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 0, 10 ) );
		panel.add( Box.createHorizontalGlue() );
		JLabel label = new JLabel(name );
		label.setHorizontalAlignment( SwingUtilities.LEFT );
		panel.add( label );

		return panel;
	}

	public JPanel createSegmentationDisplaySettingsPanel( SegmentationDisplay display )
	{
		JPanel panel = createDisplayPanel( display.getName() );

		panel.add( createSpace() );
		panel.add( createFocusButton( display, display.sourceAndConverters, display.sliceViewer.getBdvHandle() ) );
		panel.add( createOpacityButton( display.sourceAndConverters, display.getName(), display.sliceViewer.getBdvHandle() ) );
		panel.add( createButtonPlaceholder() );
		panel.add( createButtonPlaceholder() );
		panel.add( createRemoveButton( display ) );
		panel.add( createSpace() );
		panel.add( createSliceViewerVisibilityCheckbox( true, display.sourceAndConverters ) );
		panel.add( createVolumeViewerVisibilityCheckbox( display ) );
		panel.add( createWindowVisibilityCheckbox( true, display.tableViewer.getWindow() ) );
		panel.add( createScatterPlotViewerVisibilityCheckbox( display, true ) );

		return panel;
	}

	public JPanel createViewsSelectionPanel( )
	{
		final HashMap< String, View > views = moBIE2.getViews();

		Map< String, Map< String, View > > groupingsToViews = new HashMap<>(  );

		for ( String viewName : views.keySet() )
		{
			final View view = views.get( viewName );
			final String uiSelectionGroup = view.getUiSelectionGroup();
			if ( ! groupingsToViews.containsKey( uiSelectionGroup ) )
				groupingsToViews.put( uiSelectionGroup, new HashMap<>( ));
			groupingsToViews.get( uiSelectionGroup ).put( viewName, view );
		}

		JPanel containerPanel = new JPanel( new BorderLayout() );
		containerPanel.setLayout( new BoxLayout( containerPanel, BoxLayout.Y_AXIS ) );

		for ( String grouping : groupingsToViews.keySet() )
		{
			final JPanel selectionPanel = createViewSelectionPanel( moBIE2, grouping, groupingsToViews.get( grouping ) );
			containerPanel.add( selectionPanel );
		}

		viewsSelectionPanelHeight = groupingsToViews.keySet().size() * 40;

		return containerPanel;
	}

	public int getViewsSelectionPanelHeight()
	{
		return viewsSelectionPanelHeight;
	}

	private JPanel createViewSelectionPanel( MoBIE2 moBIE2, String panelName, Map< String, View > views )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JComboBox< String > comboBox = new JComboBox<>( views.keySet().toArray( new String[ 0 ] ) );

		final JButton button = createButton( ADD );
		button.addActionListener( e ->
		{
			SwingUtilities.invokeLater( () ->
			{
				final String viewName = ( String ) comboBox.getSelectedItem();
				final View view = views.get( viewName );
				moBIE2.getViewerManager().show( view );
			} );
		} );

		setComboBoxDimensions( comboBox );

		horizontalLayoutPanel.add( getJLabel( panelName ) );
		horizontalLayoutPanel.add( comboBox );
		horizontalLayoutPanel.add( button );

		return horizontalLayoutPanel;
	}

	public JPanel createLevelingPanel( double[] levelingVector )
	{
		final double[] targetNormalVector = Arrays.copyOf( levelingVector, 3 );

		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton button = createButton( LEVEL );
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

		button.addActionListener( e -> BdvUtils.levelCurrentView( moBIE2.getViewerManager().getSliceViewer().getBdvHandle(), targetNormalVector ) );

		return horizontalLayoutPanel;
	}

	public JPanel createBookmarksPanel( final BookmarkManager bookmarkManager )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();
		final JButton button = createButton( VIEW );
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
		final JButton button = createButton( MOVE );

		final JTextField jTextField = new JTextField( "120.5,115.3,201.5" );
		jTextField.setPreferredSize( new Dimension( COMBOBOX_WIDTH - 3, TEXT_FIELD_HEIGHT ) );
		jTextField.setMaximumSize( new Dimension( COMBOBOX_WIDTH - 3, TEXT_FIELD_HEIGHT ) );
		button.addActionListener( e -> BdvViewChanger.moveToLocation( moBIE2.getViewerManager().getSliceViewer().getBdvHandle(), new Location( jTextField.getText() ) ) );

		horizontalLayoutPanel.add( getJLabel( "location" ) );
		horizontalLayoutPanel.add( jTextField );
		horizontalLayoutPanel.add( button );

		return horizontalLayoutPanel;
	}

	public JPanel createInfoPanel( String projectLocation, String publicationURL )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton button = createButton( HELP );

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

	// TODO: Move to UserInterface
	private void switchDataset( String dataset )
	{
		// TODO: make sure the Swing UI sources panel is fully visible before instantiating the new BDV
		moBIE2.close();
		new MoBIE( moBIE2.getProjectLocation(), moBIE2.getOptions().dataset( dataset ) );
	}

	private static Component createSpace()
	{
		return Box.createRigidArea( PREFERRED_SPACE_SIZE );
	}
	private static Component createButtonPlaceholder()
	{
		return Box.createRigidArea( PREFERRED_BUTTON_SIZE );
	}

	private static Component createCheckboxPlaceholder()
	{
		return Box.createRigidArea( PREFERRED_CHECKBOX_SIZE );
	}

	private static JCheckBox createSliceViewerVisibilityCheckbox(
			boolean isVisible,
			final List< SourceAndConverter< ? > > sourceAndConverters )
	{
		JCheckBox checkBox = new JCheckBox( "S" );
		checkBox.setSelected( isVisible );
		checkBox.setPreferredSize( PREFERRED_CHECKBOX_SIZE );

		checkBox.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
				{
					SourceAndConverterServices.getSourceAndConverterDisplayService().setVisible( sourceAndConverter, checkBox.isSelected() );
				}
			}
		} );

		return checkBox;
	}

	private static JCheckBox createWindowVisibilityCheckbox(
			boolean isVisible,
			Window window )
	{
		JCheckBox checkBox = new JCheckBox( "T" );
		checkBox.setSelected( isVisible );
		checkBox.setPreferredSize( PREFERRED_CHECKBOX_SIZE );
		checkBox.addActionListener( e -> SwingUtilities.invokeLater( () -> window.setVisible( checkBox.isSelected() ) ) );

		window.addWindowListener(
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
		checkBox.setPreferredSize( PREFERRED_CHECKBOX_SIZE );
		checkBox.addActionListener( e ->
			SwingUtilities.invokeLater( () ->
				{
					if ( recreate.get() )
					{
						ViewerManager.showInScatterPlotViewer( display );
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

	public static JCheckBox createVolumeViewerVisibilityCheckbox( SegmentationDisplay display )
	{
		JCheckBox checkBox = new JCheckBox( "V" );
		checkBox.setSelected( display.showSelectedSegmentsIn3d() );
		checkBox.setPreferredSize( PREFERRED_CHECKBOX_SIZE );

		checkBox.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				new Thread( () -> {
					if ( checkBox.isSelected() )
					{
						display.segmentsVolumeViewer.showSegments( true );
					}
					else
					{
						display.segmentsVolumeViewer.showSegments( false );
					}
				}).start();
			}
		} );

		return checkBox;
	}

	public static JButton createFocusButton( Display display, List< SourceAndConverter< ? > > sourceAndConverters, BdvHandle bdvHandle )
	{
		JButton button = new JButton( "F" );
		button.setPreferredSize( PREFERRED_BUTTON_SIZE );

		button.addActionListener( e ->
		{
			for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
			{
				// TODO: make this work for multiple!
				final AffineTransform3D transform = new ViewerTransformAdjuster( display.sliceViewer.getBdvHandle(), sourceAndConverter ).getTransform();
				new ViewerTransformChanger( bdvHandle, transform, false, 1000 ).run();
			}
		} );

		return button;
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
					imageDisplay.getName(),
					converterSetups,
					0,   // TODO: determine somehow...
					65535 );
		} );

		return button;
	}

	public static JButton createDummyButton(  )
	{
		JButton button = new JButton( " " );
		button.setPreferredSize( PREFERRED_BUTTON_SIZE );

		button.addActionListener( e ->
		{
		} );

		return button;
	}

	public static JButton createOpacityButton( List< SourceAndConverter< ? > > sourceAndConverters, String name, BdvHandle bdvHandle )
	{
		JButton button = new JButton( "O" );
		button.setPreferredSize( PREFERRED_BUTTON_SIZE );

		button.addActionListener( e ->
		{
			UserInterfaceHelper.showOpacityDialog(
					name,
					sourceAndConverters,
					bdvHandle );
		} );

		return button;
	}

	private static JButton createColorButton( JPanel parentPanel, List< SourceAndConverter< ? > > sourceAndConverters )
	{
		JButton colorButton = new JButton( "C" );

		colorButton.setPreferredSize( PREFERRED_BUTTON_SIZE);

		colorButton.addActionListener( e ->
		{
			Color color = JColorChooser.showDialog( null, "", null );
			if ( color == null ) return;

			parentPanel.setBackground( color );

			for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
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
	private JButton createRemoveButton( Display display )
	{
		JButton removeButton = new JButton( "X" );
		removeButton.setPreferredSize( PREFERRED_BUTTON_SIZE );

		removeButton.addActionListener( e ->
		{
			moBIE2.getViewerManager().removeSourceDisplay( display );
		} );

		return removeButton;
	}
}
