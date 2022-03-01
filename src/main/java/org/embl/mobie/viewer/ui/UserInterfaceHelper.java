package org.embl.mobie.viewer.ui;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.BdvHandle;
import bdv.util.BoundedValueDouble;
import bdv.viewer.SourceAndConverter;
import com.google.gson.Gson;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.BrightnessUpdateListener;
import org.embl.mobie.viewer.*;
import org.embl.mobie.viewer.display.AbstractSourceDisplay;
import org.embl.mobie.viewer.display.AnnotatedIntervalDisplay;
import org.embl.mobie.viewer.plot.ScatterPlotViewer;
import org.embl.mobie.viewer.serialize.JsonHelper;
import org.embl.mobie.viewer.transform.ViewerTransform;
import org.embl.mobie.viewer.transform.MoBIEViewerTransformChanger;
import org.embl.mobie.viewer.color.OpacityAdjuster;
import org.embl.mobie.viewer.display.ImageSourceDisplay;
import org.embl.mobie.viewer.display.SegmentationSourceDisplay;
import org.embl.mobie.viewer.display.SourceDisplay;
import org.embl.mobie.viewer.view.View;
import de.embl.cba.tables.SwingUtils;
import de.embl.cba.tables.color.ColorUtils;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import org.embl.mobie.viewer.MoBIEUtils;
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

public class UserInterfaceHelper
{
	public static final String PROTOTYPE_DISPLAY_VALUE = "01234567890123456789";
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

	private final MoBIE moBIE;
	private int viewsSelectionPanelHeight;
	private JPanel viewSelectionPanel;
	private Map< String, Map< String, View> > groupingsToViews;
	private Map< String, JComboBox > groupingsToComboBox;

	public UserInterfaceHelper( MoBIE moBIE )
	{
		this.moBIE = moBIE;
	}

	public JPanel createDisplaySettingsContainer() {
		JPanel displaySettingsContainer = new JPanel();
		displaySettingsContainer.setLayout( new BoxLayout( displaySettingsContainer, BoxLayout.PAGE_AXIS ));
		displaySettingsContainer.setBorder( BorderFactory.createEmptyBorder() );
		displaySettingsContainer.setAlignmentX( Component.LEFT_ALIGNMENT );

		return displaySettingsContainer;
	}

	public JScrollPane createDisplaySettingsScrollPane( JPanel displaySettingsContainer ) {
		JScrollPane displaySettingsScrollPane = new JScrollPane( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		displaySettingsScrollPane.setBorder( BorderFactory.createEmptyBorder() );

		displaySettingsScrollPane.setViewportView( displaySettingsContainer );
		return displaySettingsScrollPane;
	}

	public JPanel createDisplaySettingsPanel( JScrollPane displaySettingsScrollPane ) {
		final JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout(panel, BoxLayout.Y_AXIS ) );
		panel.setAlignmentX( Component.LEFT_ALIGNMENT );
		panel.add( displaySettingsScrollPane );

		return panel;
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

		double spinnerStepSize = Math.abs( currentRangeMax - currentRangeMin ) / 100.0;

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

	public JPanel createAnnotatedIntervalDisplaySettingsPanel( AnnotatedIntervalDisplay display )
	{
		JPanel panel = createDisplayPanel( display.getName() );
		List< SourceAndConverter< ? > > sourceAndConverters = new ArrayList<>( display.sourceNameToSourceAndConverter.values() );

		// Buttons
		panel.add( createSpace() );
		panel.add( createButtonPlaceholder() ); // focus
		panel.add( createOpacityButton( sourceAndConverters, display.getName(), display.sliceViewer.getBdvHandle() ) );
		panel.add( createButtonPlaceholder() ); // color
		panel.add( createButtonPlaceholder() ); // brightness
		panel.add( createRemoveButton( display ) );
		// Checkboxes
		panel.add( createSpace() );
		panel.add( createSliceViewerVisibilityCheckbox( display.isVisible(), sourceAndConverters ) );
		panel.add( createCheckboxPlaceholder() );
		panel.add( createWindowVisibilityCheckbox( display.showTable(), display.tableViewer.getWindow() ) );
		panel.add( createScatterPlotViewerVisibilityCheckbox( display.scatterPlotViewer, display.showScatterPlot() ) );
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
				final double opacity = value.getCurrentValue();

				OpacityAdjuster.adjustOpacity( sourceAndConverter, opacity );
			}

			bdvHandle.getViewerPanel().requestRepaint();
		}
	}

	public JPanel createSelectionPanel()
	{
		final JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );

		panel.add( createInfoPanel( moBIE.getSettings().values.getProjectLocation(), moBIE.getSettings().values.getPublicationURL() ) );
		panel.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		panel.add( createDatasetSelectionPanel() );
		panel.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		panel.add( createViewsSelectionPanel() );
		panel.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		panel.add( createMoveToLocationPanel()  );

		return panel;
	}

	public JPanel createImageDisplaySettingsPanel( ImageSourceDisplay display )
	{
		JPanel panel = createDisplayPanel( display.getName() );

		// Set panel background color
		final Converter< ?, ARGBType > converter = display.sourceNameToSourceAndConverter.values().iterator().next().getConverter();
		if ( converter instanceof ColorConverter )
		{
			setPanelColor( panel, ( ( ColorConverter ) converter ).getColor() );
		}

		List< SourceAndConverter< ? > > sourceAndConverters = new ArrayList<>( display.sourceNameToSourceAndConverter.values() );

		// Buttons
		panel.add( createSpace() );
		panel.add( createFocusButton( display, sourceAndConverters, display.sliceViewer.getBdvHandle() ) );
		panel.add( createOpacityButton( sourceAndConverters, display.getName(), display.sliceViewer.getBdvHandle() ) );
		panel.add( createColorButton( panel, sourceAndConverters, display.sliceViewer.getBdvHandle() ) );
		panel.add( createImageDisplayBrightnessButton( display ) );
		panel.add( createRemoveButton( display ) );
		// Checkboxes
		panel.add( createSpace() );
		panel.add( createSliceViewerVisibilityCheckbox( display.isVisible(), sourceAndConverters ) );
		panel.add( createImageVolumeViewerVisibilityCheckbox( display ) );
		panel.add( createCheckboxPlaceholder() );
		panel.add( createCheckboxPlaceholder() );


		// make the panel color listen to color changes of the sources
		for ( SourceAndConverter< ? > sourceAndConverter : display.sourceNameToSourceAndConverter.values() )
		{
			SourceAndConverterServices.getSourceAndConverterService().getConverterSetup( sourceAndConverter ).setupChangeListeners().add( setup -> {
				// color changed listener
				setPanelColor( panel, setup.getColor() );
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

	public JPanel createSegmentationDisplaySettingsPanel( SegmentationSourceDisplay display )
	{
		JPanel panel = createDisplayPanel( display.getName() );

		List< SourceAndConverter< ? > > sourceAndConverters =
				new ArrayList<>( display.sourceNameToSourceAndConverter.values() );

		panel.add( createSpace() );
		panel.add( createFocusButton( display, sourceAndConverters, display.sliceViewer.getBdvHandle() ) );
		panel.add( createOpacityButton( sourceAndConverters, display.getName(), display.sliceViewer.getBdvHandle() ) );
		panel.add( createButtonPlaceholder() );
		panel.add( createButtonPlaceholder() );
		panel.add( createRemoveButton( display ) );
		panel.add( createSpace() );
		panel.add( createSliceViewerVisibilityCheckbox( display.isVisible(), sourceAndConverters ) );
		if ( display.tableRows != null )
		{
			// segments 3D view
			panel.add( createSegmentsVolumeViewerVisibilityCheckbox( display ) );
			// table view
			panel.add( createWindowVisibilityCheckbox( display.showTable(), display.tableViewer.getWindow() ) );
			// scatter plot view
			panel.add( createScatterPlotViewerVisibilityCheckbox( display.scatterPlotViewer, display.showScatterPlot() ) );
		}
		else
		{
			panel.add( createCheckboxPlaceholder() );
			panel.add( createCheckboxPlaceholder() );
			panel.add( createCheckboxPlaceholder() );
		}

		return panel;
	}

	public JPanel createViewsSelectionPanel( )
	{
		final Map< String, View > views = moBIE.getViews();

		groupingsToViews = new HashMap<>(  );
		groupingsToComboBox = new HashMap<>( );
		viewSelectionPanel = new JPanel( new BorderLayout() );
		viewSelectionPanel.setLayout( new BoxLayout( viewSelectionPanel, BoxLayout.Y_AXIS ) );

		addViewsToSelectionPanel( views );

		return viewSelectionPanel;
	}

	public void addViewsToSelectionPanel( Map< String, View > views ) {
		for ( String viewName : views.keySet() )
		{
			final View view = views.get( viewName );
			final String uiSelectionGroup = view.getUiSelectionGroup();
			if ( ! groupingsToViews.containsKey( uiSelectionGroup ) )
				groupingsToViews.put( uiSelectionGroup, new LinkedHashMap<>( ));
			groupingsToViews.get( uiSelectionGroup ).put( viewName, view );
		}

		final ArrayList< String > uiSelectionGroups = new ArrayList<>( groupingsToViews.keySet() );
		// sort in alphabetical order, ignoring upper/lower case
		Collections.sort( uiSelectionGroups, new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
				return s1.compareToIgnoreCase(s2);
			}
		});

		// If it's the first time, just add all the panels in order
		if ( groupingsToComboBox.keySet().size() == 0 ) {
			for (String uiSelectionGroup : uiSelectionGroups) {
				final JPanel selectionPanel = createViewSelectionPanel(moBIE, uiSelectionGroup, groupingsToViews.get(uiSelectionGroup));
				viewSelectionPanel.add(selectionPanel);
			}
		} else {
			// If there are already panels, then add new ones at the correct index to maintain alphabetical order
			Map< Integer, JPanel > indexToPanel = new HashMap<>();
			for ( String viewName : views.keySet() ) {
				String uiSelectionGroup = views.get( viewName ).getUiSelectionGroup();
				if ( groupingsToComboBox.containsKey( uiSelectionGroup ) ) {
					JComboBox comboBox = groupingsToComboBox.get( uiSelectionGroup );
					// check if a view of that name already exists: -1 means it doesn't exist
					int index = ( (DefaultComboBoxModel) comboBox.getModel() ).getIndexOf( viewName );
					if ( index == -1 ) {
						comboBox.addItem(viewName);
					}
				} else {
					final JPanel selectionPanel = createViewSelectionPanel(moBIE, uiSelectionGroup, groupingsToViews.get(uiSelectionGroup));
					int alphabeticalIndex = uiSelectionGroups.indexOf( uiSelectionGroup );
					indexToPanel.put( alphabeticalIndex, selectionPanel );
				}
			}

			if ( indexToPanel.keySet().size() > 0 ) {
				// add panels in ascending index order
				final ArrayList< Integer > sortedIndices = new ArrayList<>( indexToPanel.keySet() );
				Collections.sort( sortedIndices );
				for ( Integer index: sortedIndices ) {
					viewSelectionPanel.add( indexToPanel.get(index), index.intValue() );
				}
			}
		}

		viewsSelectionPanelHeight = groupingsToViews.keySet().size() * 40;
	}

	public Map< String, Map< String, View > > getGroupingsToViews()
	{
		return groupingsToViews;
	}

	public int getViewsSelectionPanelHeight()
	{
		return viewsSelectionPanelHeight;
	}

	public int getActionPanelHeight()
	{
		return viewsSelectionPanelHeight + 4 * 40;
	}

	public Set<String> getGroupings() {
		return groupingsToViews.keySet();
	}

	private JPanel createViewSelectionPanel(MoBIE moBIE, String panelName, Map< String, View > views )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JComboBox< String > comboBox = new JComboBox<>( views.keySet().toArray( new String[ 0 ] ) );

		final JButton button = SwingHelper.createButton( ADD );
		button.addActionListener( e ->
		{
			SwingUtilities.invokeLater( () ->
			{
				new Thread( () ->
				{
					final String viewName = ( String ) comboBox.getSelectedItem();
					final View view = views.get( viewName );
					moBIE.getViewManager().show( view );
				}).start();
			} );
		} );

		SwingHelper.setComboBoxDimensions( comboBox );

		horizontalLayoutPanel.add( SwingHelper.getJLabel( panelName ) );
		horizontalLayoutPanel.add( comboBox );
		horizontalLayoutPanel.add( button );

		groupingsToComboBox.put( panelName, comboBox );

		return horizontalLayoutPanel;
	}

	// Levelling vector for MoBIE:
	// private double[] defaultTargetNormalVector = new double[]{0.70,0.56,0.43};
	public JPanel createLevelingPanel( double[] levelingVector )
	{
		final double[] targetNormalVector = Arrays.copyOf( levelingVector, 3 );

		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton button = SwingHelper.createButton( LEVEL );
		horizontalLayoutPanel.add( button );

		button.addActionListener( e -> BdvUtils.levelCurrentView( moBIE.getViewManager().getSliceViewer().getBdvHandle(), targetNormalVector ) );

		return horizontalLayoutPanel;
	}

	public JPanel createMoveToLocationPanel( )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();
		final JButton button = SwingHelper.createButton( MOVE );

		final JTextField jTextField = new JTextField( "{\"position\":[120.5,115.3,201.5]}" );
		jTextField.setPreferredSize( new Dimension( SwingHelper.COMBOBOX_WIDTH - 3, SwingHelper.TEXT_FIELD_HEIGHT ) );
		jTextField.setMaximumSize( new Dimension( SwingHelper.COMBOBOX_WIDTH - 3, SwingHelper.TEXT_FIELD_HEIGHT ) );
		button.addActionListener( e ->
		{
			final Gson gson = JsonHelper.buildGson( false );
			final ViewerTransform viewerTransform = gson.fromJson( jTextField.getText(), ViewerTransform.class );
			MoBIEViewerTransformChanger.changeViewerTransform( moBIE.getViewManager().getSliceViewer().getBdvHandle(), viewerTransform );
		} );

		horizontalLayoutPanel.add( SwingHelper.getJLabel( "location" ) );
		horizontalLayoutPanel.add( jTextField );
		horizontalLayoutPanel.add( button );

		return horizontalLayoutPanel;
	}

	public JPanel createInfoPanel( String projectLocation, String publicationURL )
	{
		final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

		final JButton button = SwingHelper.createButton( HELP );

		final MoBIEInfo moBIEInfo = new MoBIEInfo( projectLocation, publicationURL );

		final JComboBox< String > comboBox = new JComboBox<>( moBIEInfo.getInfoChoices() );
		SwingHelper.setComboBoxDimensions( comboBox );

		button.addActionListener( e -> {
			moBIEInfo.showInfo( ( String ) comboBox.getSelectedItem() );
		} );
		comboBox.setPrototypeDisplayValue( PROTOTYPE_DISPLAY_VALUE  );

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
		final JPanel panel = SwingUtils.horizontalLayoutPanel();

		final JComboBox< String > comboBox = new JComboBox<>( moBIE.getDatasets().toArray( new String[ 0 ] ) );

		final JButton button = SwingHelper.createButton( ADD );
		button.addActionListener( e ->
		{
			SwingUtilities.invokeLater( () ->
			{
				final String dataset = ( String ) comboBox.getSelectedItem();
				moBIE.setDataset( dataset );
			} );
		} );

		comboBox.setSelectedItem( moBIE.getDatasetName() );
		SwingHelper.setComboBoxDimensions( comboBox );

		panel.add( SwingHelper.getJLabel( "dataset" ) );
		panel.add( comboBox );
		panel.add( button );

		return panel;
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

	public static JCheckBox createSegmentsVolumeViewerVisibilityCheckbox( SegmentationSourceDisplay display )
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
						display.segmentsVolumeViewer.showSegments( checkBox.isSelected() );
				}).start();
			}
		} );

		display.segmentsVolumeViewer.getListeners().add( new VisibilityListener()
		{
			@Override
			public void visibility( boolean isVisible )
			{
				SwingUtilities.invokeLater( () ->
				{
					checkBox.setSelected( isVisible );
				});
			}
		} );


		return checkBox;
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
					SourceAndConverterServices.getBdvDisplayService().setVisible( sourceAndConverter, checkBox.isSelected() );
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
		window.setVisible( isVisible );
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
			ScatterPlotViewer< ? > scatterPlotViewer,
			boolean isVisible )
	{
		JCheckBox checkBox = new JCheckBox( "P" );
		checkBox.setSelected( isVisible );
		checkBox.setPreferredSize( PREFERRED_CHECKBOX_SIZE );
		checkBox.addActionListener( e ->
			SwingUtilities.invokeLater( () ->
				{
					if ( checkBox.isSelected() )
						scatterPlotViewer.show();
					else
						scatterPlotViewer.hide();
				} ) );

		scatterPlotViewer.getListeners().add( new VisibilityListener()
		{
			@Override
			public void visibility( boolean isVisible )
			{
				SwingUtilities.invokeLater( () ->
				{
					checkBox.setSelected( isVisible );
				});
			}
		} );

		return checkBox;
	}

	public static JCheckBox createImageVolumeViewerVisibilityCheckbox( ImageSourceDisplay display )
	{
		JCheckBox checkBox = new JCheckBox( "V" );
		checkBox.setSelected( display.showImagesIn3d() );
		checkBox.setPreferredSize( PREFERRED_CHECKBOX_SIZE );

		checkBox.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				new Thread( () -> {
						display.imageVolumeViewer.showImages( checkBox.isSelected() );
				}).start();
			}
		} );

		display.imageVolumeViewer.getListeners().add( new VisibilityListener()
		{
			@Override
			public void visibility( boolean isVisible )
			{
				SwingUtilities.invokeLater( () ->
				{
					checkBox.setSelected( isVisible );
				});
			}
		} );


		return checkBox;
	}

	public static JButton createFocusButton( AbstractSourceDisplay sourceDisplay, List< SourceAndConverter< ? > > sourceAndConverters, BdvHandle bdvHandle )
	{
		JButton button = new JButton( "F" );
		button.setPreferredSize( PREFERRED_BUTTON_SIZE );

		button.addActionListener( e ->
		{
			if( sourceAndConverters.size() > 1 )
			{
				// FIXME Does not always work...
				final SourceAndConverter[] array = sourceAndConverters.toArray( new SourceAndConverter[ 0 ] );
				final AffineTransform3D transform = new ViewerTransformAdjuster( sourceDisplay.sliceViewer.getBdvHandle(), array ).getTransformMultiSources();
				new ViewerTransformChanger( bdvHandle, transform, false, 1000 ).run();
			}
			else
			{
				final AffineTransform3D transform = new ViewerTransformAdjuster( sourceDisplay.sliceViewer.getBdvHandle(), sourceAndConverters.get( 0 ) ).getTransform();
				new ViewerTransformChanger( bdvHandle, transform, false, 1000 ).run();
			}
		} );

		return button;
	}

	public static JButton createImageDisplayBrightnessButton( ImageSourceDisplay imageDisplay )
	{
		JButton button = new JButton( "B" );
		button.setPreferredSize( PREFERRED_BUTTON_SIZE );

		button.addActionListener( e ->
		{
			final ArrayList< ConverterSetup > converterSetups = new ArrayList<>();
			for ( SourceAndConverter< ? > sourceAndConverter : imageDisplay.sourceNameToSourceAndConverter.values() )
			{
				converterSetups.add( SourceAndConverterServices.getSourceAndConverterService().getConverterSetup( sourceAndConverter ) );
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

	private static JButton createColorButton( JPanel parentPanel, List< SourceAndConverter< ? > > sourceAndConverters, BdvHandle bdvHandle )
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

			bdvHandle.getViewerPanel().requestRepaint();
		} );

		return colorButton;
	}

	private void setPanelColor( JPanel panel, ARGBType argbType )
	{
		final Color color = ColorUtils.getColor( argbType );
		if ( color != null )
		{
			panel.setOpaque( true );
			panel.setBackground( color );
		}
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

	private JButton createRemoveButton( SourceDisplay sourceDisplay )
	{
		JButton removeButton = new JButton( "X" );
		removeButton.setPreferredSize( PREFERRED_BUTTON_SIZE );

		removeButton.addActionListener( e ->
		{
			moBIE.getViewManager().removeSourceDisplay( sourceDisplay );
		} );

		return removeButton;
	}

	public static String tidyString( String string ) {
		string = string.trim();
		String tidyString = string.replaceAll("\\s+","_");

		if ( !string.equals(tidyString) ) {
			MoBIEUtils.log( "Spaces were removed from name, and replaced by _");
		}

		// check only contains alphanumerics, or _ -
		if ( !tidyString.matches("^[a-zA-Z0-9_-]+$") ) {
			MoBIEUtils.log( "Names must only contain letters, numbers, _ or -. Please try again " +
					"with a different name.");
			tidyString = null;
		}

		return tidyString;
	}
}
