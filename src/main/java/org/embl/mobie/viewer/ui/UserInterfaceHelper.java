/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.viewer.ui;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.BdvHandle;
import bdv.util.BoundedValueDouble;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.SwingUtils;
import de.embl.cba.tables.color.ColorUtils;
import ij.IJ;
import ij.gui.GenericDialog;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIEHelper;
import org.embl.mobie.viewer.MoBIEHelper.FileLocation;
import org.embl.mobie.viewer.MoBIEInfo;
import org.embl.mobie.viewer.command.ConfigureLabelRenderingCommand;
import org.embl.mobie.viewer.command.ConfigureSpotRenderingCommand;
import org.embl.mobie.viewer.serialize.display.SpotDisplay;
import org.embl.mobie.viewer.serialize.display.VisibilityListener;
import org.embl.mobie.viewer.color.OpacityHelper;
import org.embl.mobie.viewer.color.opacity.OpacityAdjuster;
import org.embl.mobie.viewer.serialize.display.AbstractDisplay;
import org.embl.mobie.viewer.serialize.display.ImageDisplay;
import org.embl.mobie.viewer.serialize.display.RegionDisplay;
import org.embl.mobie.viewer.serialize.display.SegmentationDisplay;
import org.embl.mobie.viewer.serialize.display.Display;
import org.embl.mobie.viewer.plot.ScatterPlotView;
import org.embl.mobie.viewer.transform.MoBIEViewerTransformAdjuster;
import org.embl.mobie.viewer.transform.SliceViewLocationChanger;
import org.embl.mobie.viewer.transform.ViewerTransform;
import org.embl.mobie.viewer.serialize.View;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformChanger;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.embl.mobie.viewer.ui.SwingHelper.TEXT_FIELD_HEIGHT;
import static org.embl.mobie.viewer.ui.SwingHelper.selectionDialog;

public class UserInterfaceHelper
{
	private static final Dimension PREFERRED_BUTTON_SIZE = new Dimension( 30, 30 );
	private static final Dimension PREFERRED_CHECKBOX_SIZE = new Dimension( 40, 30 );
	private static final Dimension PREFERRED_SPACE_SIZE = new Dimension( 10, 30 );
	private static final String MOVE = "move";
	private static final String HELP = "show";
	private static final String LEVEL = "level";
	private static final String ADD = "view";
	public static final int SPACING = 20;
	public static File lastSelectedDir;

	private final MoBIE moBIE;
	private int viewsSelectionPanelHeight;
	private JPanel viewSelectionPanel;
	private Map< String, Map< String, View > > groupingsToViews;
	private Map< String, JComboBox > groupingsToComboBox;

	public UserInterfaceHelper( MoBIE moBIE )
	{
		this.moBIE = moBIE;
	}

	public static FileLocation loadFromProjectOrFileSystemDialog() {
		final GenericDialog gd = new GenericDialog("Choose source");
		gd.addChoice("Load from", new String[]{ FileLocation.Project.toString(), FileLocation.FileSystem.toString()}, FileLocation.Project.toString());
		gd.showDialog();
		if (gd.wasCanceled()) return null;
		return FileLocation.valueOf(gd.getNextChoice());
	}

	// objectName is used for the dialog labels e.g. 'table', 'bookmark' etc...
	public static String selectDirectoryPath( String objectName, boolean open ) {
		final JFileChooser jFileChooser = new JFileChooser( lastSelectedDir );
		jFileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
		jFileChooser.setDialogTitle( "Select " + objectName );
		return selectPath( jFileChooser, open );
	}

	public static String selectPath( JFileChooser jFileChooser, boolean openOrSave ) {
		final AtomicBoolean isDone = new AtomicBoolean( false );
		final String[] path = new String[ 1 ];
		Runnable r = () -> {
			if ( openOrSave ) {
				path[0] = selectOpenPathFromFileSystem( jFileChooser);
			} else {
				path[0] = selectSavePathFromFileSystem( jFileChooser );
			}
			isDone.set( true );
		};

		SwingUtilities.invokeLater(r);

		while ( ! isDone.get() ){
			try {
				Thread.sleep( 100 );
			} catch ( InterruptedException e )
			{ e.printStackTrace(); }
		};

		return path[ 0 ];
	}

	// objectName is used for the dialog labels e.g. 'table', 'bookmark' etc...
	public static String selectFilePath( String fileExtension, String objectName, boolean openOrSave ) {
		final JFileChooser jFileChooser = new JFileChooser( lastSelectedDir );
		if ( fileExtension != null ) {
			jFileChooser.setFileFilter( new FileNameExtensionFilter( fileExtension, fileExtension ) );
		}
		jFileChooser.setDialogTitle( "Select " + objectName );
		return selectPath( jFileChooser, openOrSave );
	}

	public static String selectOpenPathFromFileSystem( JFileChooser jFileChooser ) {
		String filePath = null;
		if (jFileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			filePath = jFileChooser.getSelectedFile().getAbsolutePath();
			setLastSelectedDir( filePath );
		}
		return filePath;
	}

	public static void setLastSelectedDir( String filePath ) {
		File selectedFile = new File( filePath );
		if ( selectedFile.isDirectory() ) {
			lastSelectedDir = selectedFile;
		} else {
			lastSelectedDir = selectedFile.getParentFile();
		}
	}

	public static String selectSavePathFromFileSystem( JFileChooser jFileChooser )
	{
		String filePath = null;
		if (jFileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
			filePath = jFileChooser.getSelectedFile().getAbsolutePath();
			setLastSelectedDir( filePath );
		}
		return filePath;
	}

	public static String selectPathFromProject( String directory, String objectName ) {
		if ( directory == null ) {
			return null;
		}

		String[] fileNames = IOHelper.getFileNames( directory );
		String fileName = selectionDialog( fileNames, objectName );
		if ( fileName != null ) {
			return IOHelper.combinePath( directory, fileName );
		} else {
			return null;
		}
	}

	public static String selectTableFileNameFromProjectDialog( Collection< String > directories, String objectName )
	{
		if ( directories == null )
			return null;

		if ( directories.size() > 1)
		{
			// when there are multiple directories,
			// we only allow selection of table file names
			// that are present in all directories
			return MoBIEHelper.chooseValidTableFileName( directories, objectName );
		}
		else
		{
			final String directory = directories.iterator().next();
			String[] fileNames = IOHelper.getFileNames( directory );
			if ( fileNames == null )
				throw new RuntimeException("Could not find any files at " + directory );
			return selectionDialog( fileNames, objectName );
		}
	}

	private static String chooseValidTableFileName( Collection< String > directories, String objectName ) {
		ArrayList< String > commonFileNames = MoBIEHelper.getCommonFileNames( directories );
		if ( commonFileNames.size() > 0 ) {
			String[] choices = new String[commonFileNames.size()];
			for (int i = 0; i < choices.length; i++) {
				choices[i] = commonFileNames.get(i);
			}
			return selectionDialog(choices, objectName);
		} else {
			return null;
		}
	}

	// find file names that occur in all directories
	private static ArrayList< String > getCommonFileNames( Collection< String > directories )
	{
		Map<String, Integer> fileNameCounts = new HashMap<>();
		ArrayList<String> commonFileNames = new ArrayList<>();

		for ( String directory: directories ) {
			String[] directoryFileNames = IOHelper.getFileNames( directory );
			for ( String directoryFileName: directoryFileNames ) {
				if ( fileNameCounts.containsKey( directoryFileName ) ) {
					int count = fileNameCounts.get(directoryFileName);
					fileNameCounts.put( directoryFileName, count + 1 );
				} else {
					fileNameCounts.put( directoryFileName, 1 );
				}
			}
		}

		for ( String fileName: fileNameCounts.keySet() ) {
			if ( fileNameCounts.get( fileName ) == directories.size() ) {
				commonFileNames.add( fileName );
			}
		}
		return commonFileNames;
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

	public static void showContrastLimitsDialog(
			String name,
			List< ConverterSetup > converterSetups )
	{
		JFrame frame = new JFrame( name );
		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

		final double currentContrastLimitsMin = converterSetups.get( 0 ).getDisplayRangeMin();
		final double currentContrastLimitsMax = converterSetups.get( 0 ).getDisplayRangeMax();
		final double absCurrentRange = Math.abs( currentContrastLimitsMax - currentContrastLimitsMin );

		final double rangeFactor = 1.0; // could be changed...

		final double rangeMin = currentContrastLimitsMin - rangeFactor * absCurrentRange;
		final double rangeMax = currentContrastLimitsMax + rangeFactor * absCurrentRange;

		final BoundedValueDouble min =
				new BoundedValueDouble(
						rangeMin,
						rangeMax,
						currentContrastLimitsMin );

		final BoundedValueDouble max =
				new BoundedValueDouble(
						rangeMin,
						rangeMax,
						currentContrastLimitsMax );

		double spinnerStepSize = absCurrentRange / 100.0;

		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.PAGE_AXIS ) );
		final SliderPanelDouble minSlider =
				new SliderPanelDouble( "Min", min, spinnerStepSize );
		minSlider.setNumColummns( 10 );

		// FIXME: adapt the number of decimal places to the
		//  current range
		minSlider.setDecimalFormat( "#####.####" );
		//minSlider.setDecimalFormat( "####E0" );

		final SliderPanelDouble maxSlider =
				new SliderPanelDouble( "Max", max, spinnerStepSize );
		maxSlider.setNumColummns( 10 );
		maxSlider.setDecimalFormat( "#####.####" );
		//maxSlider.setDecimalFormat( "####E0" );

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
		//   Maybe we should rather operate on the coloring model that is
		//   wrapped in the converter?
		final double current = ( ( OpacityAdjuster ) sourceAndConverters.get( 0 ).getConverter()).getOpacity();

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

	public JPanel createRegionDisplaySettingsPanel( RegionDisplay display )
	{
		JPanel panel = createDisplayPanel( display.getName() );
		List< SourceAndConverter< ? > > sourceAndConverters = new ArrayList<>( display.nameToSourceAndConverter.values() );

		// Buttons
		panel.add( space() );
		panel.add( createFocusButton( display, display.sliceViewer.getBdvHandle(), sourceAndConverters.stream().map( sac -> sac.getSpimSource() ).collect( Collectors.toList() ) ) );
		panel.add( createOpacityButton( sourceAndConverters, display.getName(), display.sliceViewer.getBdvHandle() ) );
		panel.add( createButtonPlaceholder() ); // color
		panel.add( createButtonPlaceholder() ); // brightness
		panel.add( createLabelRenderingSettingsButton( sourceAndConverters ) ); // special settings
		panel.add( createRemoveButton( display ) );
		// Checkboxes
		panel.add( space() );
		panel.add( createSliceViewerVisibilityCheckbox( display.isVisible(), sourceAndConverters ) );
		panel.add( createCheckboxPlaceholder() );
		panel.add( createWindowVisibilityCheckbox( display.showTable(), display.tableView.getWindow() ) );
		panel.add( createScatterPlotViewerVisibilityCheckbox( display.scatterPlotView, display.showScatterPlot() ) );
		return panel;
	}

	public JPanel createSpotDisplaySettingsPanel( SpotDisplay display )
	{
		JPanel panel = createDisplayPanel( display.getName() );
		List< SourceAndConverter< ? > > sourceAndConverters = new ArrayList<>( display.nameToSourceAndConverter.values() );

		// Buttons
		panel.add( space() );
		panel.add( createFocusButton( display, display.sliceViewer.getBdvHandle(), sourceAndConverters.stream().map( sac -> sac.getSpimSource() ).collect( Collectors.toList() ) ) );
		panel.add( createOpacityButton( sourceAndConverters, display.getName(), display.sliceViewer.getBdvHandle() ) );
		panel.add( createButtonPlaceholder() ); // color
		panel.add( createButtonPlaceholder() ); // brightness
		panel.add( createSpotSettingsButton( sourceAndConverters ) ); // special settings
		panel.add( createRemoveButton( display ) );
		// Checkboxes
		panel.add( space() );
		panel.add( createSliceViewerVisibilityCheckbox( display.isVisible(), sourceAndConverters ) );
		panel.add( createCheckboxPlaceholder() );
		panel.add( createWindowVisibilityCheckbox( display.showTable(), display.tableView.getWindow() ) );
		panel.add( createScatterPlotViewerVisibilityCheckbox( display.scatterPlotView, display.showScatterPlot() ) );
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
				OpacityHelper.setOpacity( sourceAndConverter, opacity );
			}

			bdvHandle.getViewerPanel().requestRepaint();
		}
	}

	public JPanel createSelectionPanel()
	{
		final JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );

		panel.add( createInfoPanel( moBIE.getSettings().values.getProjectLocation(), moBIE.getSettings().values.getPublicationURL() ) );

		if ( moBIE.getDatasets().size() > 1 )
		{
			panel.add( new JSeparator( SwingConstants.HORIZONTAL ) );
			panel.add( createDatasetSelectionPanel() );
		}

		panel.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		panel.add( createViewsSelectionPanel() );

		panel.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		panel.add( createMoveToLocationPanel( moBIE.getDataset().defaultLocation )  );
		panel.add( createClearAndSourceNamesOverlayPanel( moBIE ) );

		return panel;
	}

	public JPanel createImageDisplaySettingsPanel( ImageDisplay< ? > display )
	{
		JPanel panel = createDisplayPanel( display.getName() );

		// Set panel background color
		final Converter< ?, ARGBType > converter = display.nameToSourceAndConverter.values(). iterator().next().getConverter();
		if ( converter instanceof ColorConverter )
		{
			setPanelColor( panel, ( ( ColorConverter ) converter ).getColor() );
		}

		List< SourceAndConverter< ? > > sourceAndConverters = new ArrayList<>( display.nameToSourceAndConverter.values() );

		// Buttons
		panel.add( space() );
		panel.add( createFocusButton( display, display.sliceViewer.getBdvHandle(), sourceAndConverters.stream().map( sac -> sac.getSpimSource() ).collect( Collectors.toList() ) ) );
		panel.add( createOpacityButton( sourceAndConverters, display.getName(), display.sliceViewer.getBdvHandle() ) );
		panel.add( createColorButton( panel, sourceAndConverters, display.sliceViewer.getBdvHandle() ) );
		panel.add( createImageDisplayBrightnessButton( display ) );
		panel.add( createButtonPlaceholder() );
		panel.add( createRemoveButton( display ) );
		// Checkboxes
		panel.add( space() );
		panel.add( createSliceViewerVisibilityCheckbox( display.isVisible(), sourceAndConverters ) );
		panel.add( createImageVolumeViewerVisibilityCheckbox( display ) );
		panel.add( createCheckboxPlaceholder() );
		panel.add( createCheckboxPlaceholder() );


		// make the panel color listen to color changes of the sources
		for ( SourceAndConverter< ? > sourceAndConverter : display.nameToSourceAndConverter.values() )
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

	public JPanel createSegmentationDisplaySettingsPanel( SegmentationDisplay display )
	{
		JPanel panel = createDisplayPanel( display.getName() );

		List< SourceAndConverter< ? > > sourceAndConverters =
				new ArrayList<>( display.nameToSourceAndConverter.values() );

		panel.add( space() );
		panel.add( createFocusButton( display, display.sliceViewer.getBdvHandle(), sourceAndConverters.stream().map( sac -> sac.getSpimSource() ).collect( Collectors.toList() ) ) );
		panel.add( createOpacityButton( sourceAndConverters, display.getName(), display.sliceViewer.getBdvHandle() ) );
		panel.add( createButtonPlaceholder() );
		panel.add( createButtonPlaceholder() );
		panel.add( createLabelRenderingSettingsButton( sourceAndConverters ) ); // special settings
		panel.add( createRemoveButton( display ) );
		panel.add( space() );
		panel.add( createSliceViewerVisibilityCheckbox( display.isVisible(), sourceAndConverters ) );
		if ( display.getAnnData() != null )
		{
			// segments 3D view
			panel.add( createSegmentsVolumeViewerVisibilityCheckbox( display ) );
			// table view
			panel.add( createWindowVisibilityCheckbox( display.showTable(), display.tableView.getWindow() ) );
			// scatter plot view
			panel.add( createScatterPlotViewerVisibilityCheckbox( display.scatterPlotView, display.showScatterPlot() ) );
		}
		else
		{
			panel.add( createCheckboxPlaceholder() );
			panel.add( createCheckboxPlaceholder() );
			panel.add( createCheckboxPlaceholder() );
		}

		return panel;
	}

	private JButton createLabelRenderingSettingsButton( List< SourceAndConverter< ? > > sourceAndConverters )
	{
		JButton button = new JButton( "S" );
		button.setPreferredSize( PREFERRED_BUTTON_SIZE );
		button.addActionListener( e ->
		{
			final SourceAndConverter[] sacArray = sourceAndConverters.toArray( new SourceAndConverter[ 0 ] );

			MoBIE.getCommandService().run( ConfigureLabelRenderingCommand.class, true, "sourceAndConverters", sacArray );
		} );
		return button;
	}

	private JButton createSpotSettingsButton( List< SourceAndConverter< ? > > sourceAndConverters )
	{
		JButton button = new JButton( "S" );
		button.setPreferredSize( PREFERRED_BUTTON_SIZE );
		button.addActionListener( e ->
		{
			final SourceAndConverter[] sacArray = sourceAndConverters.toArray( new SourceAndConverter[ 0 ] );
			MoBIE.getCommandService().run( ConfigureSpotRenderingCommand.class, true, "sourceAndConverters", sacArray );
		} );
		return button;
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

	public void addViewsToSelectionPanel( Map< String, View > views )
	{
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

	private JPanel createClearAndSourceNamesOverlayPanel( MoBIE moBIE )
	{
		final JPanel panel = SwingUtils.horizontalLayoutPanel();

		final JButton button = SwingHelper.createButton( "clear", new Dimension( 80, TEXT_FIELD_HEIGHT ) );
		button.addActionListener( e ->
		{
			SwingUtilities.invokeLater( () ->
			{
				new Thread( () ->
				{
					moBIE.getViewManager().removeAllSourceDisplays( true );
				}).start();
			} );
		} );

		JCheckBox checkBox = new JCheckBox( "overlay names" );
		checkBox.setSelected( moBIE.initiallyShowSourceNames );
		checkBox.addActionListener( e -> new Thread( () ->
		{
			moBIE.getViewManager().getSliceViewer().getSourceNameRenderer().setActive( checkBox.isSelected() );
		}).start() );

		panel.add( checkBox );
		panel.add( space() );
		panel.add( button );
		return panel;
	}

	private JPanel createViewSelectionPanel( MoBIE moBIE, String panelName, Map< String, View > views )
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
					view.setName( viewName );
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

	public JPanel createMoveToLocationPanel( ViewerTransform transform )
	{
		final JPanel panel = SwingUtils.horizontalLayoutPanel();
		final JButton button = SwingHelper.createButton( MOVE );
		final JTextField jTextField = new JTextField( ViewerTransform.toString( transform ) );
		jTextField.setPreferredSize( new Dimension( SwingHelper.COMBOBOX_WIDTH - 3, TEXT_FIELD_HEIGHT ) );
		jTextField.setMaximumSize( new Dimension( SwingHelper.COMBOBOX_WIDTH - 3, TEXT_FIELD_HEIGHT ) );
		button.addActionListener( e ->
		{
			ViewerTransform viewerTransform = ViewerTransform.toViewerTransform( jTextField.getText() );
			SliceViewLocationChanger.changeLocation( this.moBIE.getViewManager().getSliceViewer().getBdvHandle(), viewerTransform );
		} );

		panel.add( SwingHelper.getJLabel( "location" ) );
		panel.add( jTextField );
		panel.add( button );

		return panel;
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
		final URL resource = UserInterfaceHelper.class.getResource( "/mobie.png" );
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

	private static Component space()
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

	public static JCheckBox createSegmentsVolumeViewerVisibilityCheckbox( SegmentationDisplay display )
	{
		JCheckBox checkBox = new JCheckBox( "V" );
		checkBox.setSelected( display.showSelectedSegmentsIn3d() );
		checkBox.setPreferredSize( PREFERRED_CHECKBOX_SIZE );

		checkBox.addActionListener( e -> new Thread( () ->
		{
				display.segmentsVolumeViewer.showSegments( checkBox.isSelected(), false );
		}).start() );

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
			ScatterPlotView< ? > scatterPlotView,
			boolean isVisible )
	{
		JCheckBox checkBox = new JCheckBox( "P" );
		checkBox.setSelected( isVisible );
		checkBox.setPreferredSize( PREFERRED_CHECKBOX_SIZE );
		checkBox.addActionListener( e ->
			SwingUtilities.invokeLater( () ->
				{
					if ( checkBox.isSelected() )
						scatterPlotView.show();
					else
						scatterPlotView.hide();
				} ) );

		scatterPlotView.getListeners().add( new VisibilityListener()
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

	public static JCheckBox createImageVolumeViewerVisibilityCheckbox( ImageDisplay display )
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

	public static JButton createFocusButton( AbstractDisplay sourceDisplay, BdvHandle bdvHandle, List< Source< ? > > sources )
	{
		JButton button = new JButton( "F" );
		button.setPreferredSize( PREFERRED_BUTTON_SIZE );

		button.addActionListener( e ->
		{
//			final AffineTransform3D transform = new ViewerTransformAdjuster(  sourceDisplay.sliceViewer.getBdvHandle(), sourceAndConverters.get( 0 ) ).getTransform();

			final AffineTransform3D transform = new MoBIEViewerTransformAdjuster( sourceDisplay.sliceViewer.getBdvHandle(), sources ).getMultiSourceTransform();
			new ViewerTransformChanger( bdvHandle, transform, false, SliceViewLocationChanger.animationDurationMillis ).run();
		} );

		return button;
	}

	public static JButton createImageDisplayBrightnessButton( ImageDisplay< ? > imageDisplay )
	{
		JButton button = new JButton( "B" );
		button.setPreferredSize( PREFERRED_BUTTON_SIZE );

		button.addActionListener( e ->
		{
			final ArrayList< ConverterSetup > converterSetups = new ArrayList<>();
			for ( SourceAndConverter< ? > sourceAndConverter : imageDisplay.nameToSourceAndConverter.values() )
			{
				converterSetups.add( SourceAndConverterServices.getSourceAndConverterService().getConverterSetup( sourceAndConverter ) );
			}

			UserInterfaceHelper.showContrastLimitsDialog(
					imageDisplay.getName(),
					converterSetups );
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

	private JButton createRemoveButton( Display display )
	{
		JButton removeButton = new JButton( "X" );
		removeButton.setPreferredSize( PREFERRED_BUTTON_SIZE );

		removeButton.addActionListener( e ->
		{
			// remove the display but do not close the ImgLoader
			// because some derived sources may currently be shown
			moBIE.getViewManager().removeDisplay( display, false );
		} );

		return removeButton;
	}

	public static String tidyString( String string ) {
		string = string.trim();
		String tidyString = string.replaceAll("\\s+","_");

		if ( !string.equals(tidyString) ) {
			IJ.log( "Spaces were removed from name, and replaced by _");
		}

		// check only contains alphanumerics, or _ -
		if ( !tidyString.matches("^[a-zA-Z0-9_-]+$") ) {
			IJ.log( "Names must only contain letters, numbers, _ or -. Please try again " +
					"with a different name.");
			tidyString = null;
		}

		return tidyString;
	}
}
