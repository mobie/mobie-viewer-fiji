/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
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
package org.embl.mobie.ui;

import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.BdvHandle;
import bdv.util.BoundedValueDouble;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import ij.gui.GenericDialog;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import org.embl.mobie.command.context.ConfigureSegmentRenderingCommand;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.MoBIE;
import org.embl.mobie.lib.io.FileLocation;
import org.embl.mobie.lib.Services;
import org.embl.mobie.lib.color.ColorHelper;
import org.embl.mobie.lib.color.OpacityHelper;
import org.embl.mobie.command.context.ConfigureImageRenderingCommand;
import org.embl.mobie.command.context.ConfigureLabelRenderingCommand;
import org.embl.mobie.command.context.ConfigureSpotRenderingCommand;
import org.embl.mobie.lib.plot.ScatterPlotView;
import org.embl.mobie.lib.serialize.Project;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.lib.serialize.display.*;
import org.embl.mobie.lib.table.AnnData;
import org.embl.mobie.lib.table.TableView;
import org.embl.mobie.lib.transform.viewer.MoBIEViewerTransformAdjuster;
import org.embl.mobie.lib.transform.viewer.ViewerTransformChanger;
import org.embl.mobie.lib.transform.viewer.ViewerTransform;
import org.embl.mobie.lib.volume.ImageVolumeViewer;
import org.embl.mobie.lib.volume.SegmentVolumeViewer;
import org.jetbrains.annotations.NotNull;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class UserInterfaceHelper
{
	public static final String PROTOTYPE_DISPLAY_VALUE = "01234567890123456789";
	private static final Dimension PREFERRED_BUTTON_SIZE = new Dimension( 30, 30 );
	private static final Dimension PREFERRED_CHECKBOX_SIZE = new Dimension( 40, 30 );
	private static final Dimension PREFERRED_SPACE_SIZE = new Dimension( 10, 30 );
	private static final String MOVE = "move";
	private static final String HELP = "show";
	private static final String VIEW = "view";
	public static final int SPACING = 10;
	public static File lastSelectedDir;
	private final MoBIE moBIE;
	private int viewsSelectionPanelHeight;
	private JPanel viewSelectionPanel;
	private Map< String, Map< String, View > > groupingsToViews;
	private Map< String, JComboBox< String > > groupingsToComboBox;
	private Map< String, JPanel > groupingsToPanels;
	private JCheckBox overlayNamesCheckbox;

	public UserInterfaceHelper( MoBIE moBIE )
	{
		this.moBIE = moBIE;
	}

	public JCheckBox getOverlayNamesCheckbox()
	{
		return overlayNamesCheckbox;
	}


	public static void closeWindowByName(String windowTitle) {
		Frame[] frames = Frame.getFrames();
		for (Frame frame : frames) {
			if ( frame.getTitle().equals( windowTitle ) ) {
				frame.dispose();
				break;
			}
		}

		Window[] windows = JDialog.getWindows();
		for ( Window window : windows )
		{
			if ( window instanceof javax.swing.JDialog)
			{
				String title = ( ( JDialog ) window ).getTitle();
				if ( title.equals( windowTitle ) )
				{
					window.dispose();
					break;
				}
			}
		}

	}

	public static FileLocation loadFromProjectOrFileSystemDialog() {
		return loadFromProjectOrFileSystemDialog("Load from");
	}

	public static FileLocation loadFromProjectOrFileSystemDialog( String dialogText ) {
		final GenericDialog gd = new GenericDialog("Choose source");
		gd.addChoice(dialogText, new String[]{ FileLocation.CurrentProject.toString(), FileLocation.ExternalFile.toString()}, FileLocation.CurrentProject.toString());
		gd.showDialog();
		if ( gd.wasCanceled() ) return null;
		return FileLocation.fromString( gd.getNextChoice() );
	}

	// objectName is used for the dialog labels e.g. 'table', 'bookmark' etc...
	// TODO: Can we use this also to fetch a S3 address?
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
		String fileName = SwingHelper.selectionDialog( fileNames, objectName );
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
			return chooseValidTableFileName( directories, objectName );
		}
		else
		{
			final String directory = directories.iterator().next();
			String[] fileNames = IOHelper.getFileNames( directory );
			if ( fileNames == null )
				throw new RuntimeException("Could not find any files at " + directory );
			return SwingHelper.selectionDialog( fileNames, objectName );
		}
	}

	private static String chooseValidTableFileName( Collection< String > directories, String objectName ) {
		ArrayList< String > commonFileNames = getCommonFileNames( directories );
		if ( commonFileNames.size() > 0 ) {
			String[] choices = new String[commonFileNames.size()];
			for (int i = 0; i < choices.length; i++) {
				choices[i] = commonFileNames.get(i);
			}
			return SwingHelper.selectionDialog(choices, objectName);
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
                    fileNameCounts.compute( directoryFileName, ( k, count ) -> count + 1 );
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

	public JPanel createContainerPanel() {
		JPanel containerPanel = new JPanel();
		containerPanel.setLayout( new BoxLayout( containerPanel, BoxLayout.PAGE_AXIS ));
		containerPanel.setBorder( BorderFactory.createEmptyBorder() );
		containerPanel.setAlignmentX( Component.LEFT_ALIGNMENT );
		return containerPanel;
	}

	public JScrollPane createScrollPane( JPanel container ) {
		JScrollPane scrollPane = new JScrollPane( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		scrollPane.setBorder( BorderFactory.createEmptyBorder() );
		scrollPane.setViewportView( container );
		return scrollPane;
	}

	public JPanel createPanel( JScrollPane scrollPane )
	{
		final JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );
		panel.setAlignmentX( Component.LEFT_ALIGNMENT );
		panel.add( scrollPane );
		return panel;
	}

	public JPanel createRegionDisplaySettingsPanel( RegionDisplay display )
	{
		JPanel panel = createDisplayPanel( display.getName() );
		List< SourceAndConverter< ? > > sourceAndConverters = display.sourceAndConverters();

		// Buttons
		panel.add( space() );
		panel.add( createFocusButton( display, display.sliceViewer.getBdvHandle(), sourceAndConverters.stream().map( sac -> sac.getSpimSource() ).collect( Collectors.toList() ) ) );
		panel.add( createContrastButton( sourceAndConverters, display.getName(), display.sliceViewer.getBdvHandle(), false ) );
		panel.add( createButtonPlaceholder() ); // color
		panel.add( createLabelRenderingSettingsButton( sourceAndConverters ) ); // special settings
		panel.add( createRemoveButton( display ) );
		// Checkboxes
		panel.add( space() );
		panel.add( createSliceViewerVisibilityCheckbox( display.isVisible(), sourceAndConverters ) );
		panel.add( createCheckboxPlaceholder() );
		panel.add( createCheckboxPlaceholder() );
		panel.add( createTableVisibilityCheckbox( display.tableView, display.showTable() ) );
		panel.add( createScatterPlotViewerVisibilityCheckbox( display.scatterPlotView, display.showScatterPlot() ) );
		return panel;
	}

	public JPanel createSpotDisplaySettingsPanel( SpotDisplay display )
	{
		JPanel panel = createDisplayPanel( display.getName() );
		List< SourceAndConverter< ? > > sourceAndConverters = display.sourceAndConverters();

		// Buttons
		panel.add( space() );
		panel.add( createFocusButton( display, display.sliceViewer.getBdvHandle(), sourceAndConverters.stream().map( sac -> sac.getSpimSource() ).collect( Collectors.toList() ) ) );
		panel.add( createContrastButton( sourceAndConverters, display.getName(), display.sliceViewer.getBdvHandle(), false ) );
		panel.add( createButtonPlaceholder() ); // color
		panel.add( createSpotSettingsButton( sourceAndConverters ) ); // special settings
		panel.add( createRemoveButton( display ) );
		// Checkboxes
		panel.add( space() );
		panel.add( createSliceViewerVisibilityCheckbox( display.isVisible(), sourceAndConverters ) );
		panel.add( createCheckboxPlaceholder() );
		panel.add( createBVVVisibilityCheckbox( display, sourceAndConverters ) );
		panel.add( createTableVisibilityCheckbox( display.tableView, display.showTable() ) );
		panel.add( createScatterPlotViewerVisibilityCheckbox( display.scatterPlotView, display.showScatterPlot() ) );
		return panel;
	}

	public static class OpacityUpdateListener implements BoundedValueDouble.UpdateListener
	{
		final private ContrastAdjustmentManager contrastAdjustmentManager;
		private final BdvHandle bdvHandle;
		final private BoundedValueDouble value;
		private final SliderPanelDouble slider;

		public OpacityUpdateListener( BoundedValueDouble value,
									  SliderPanelDouble slider,
									  ContrastAdjustmentManager contrastAdjustmentManager,
									  BdvHandle bdvHandle )
		{
			this.value = value;
			this.slider = slider;
			this.contrastAdjustmentManager = contrastAdjustmentManager;
			this.bdvHandle = bdvHandle;
		}

		@Override
		public void update()
		{
			slider.update();

			List< ? extends SourceAndConverter< ? > > sourceAndConverters = contrastAdjustmentManager.getAdjustable();

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

		panel.add( createInfoPanel( moBIE.getProjectLocation(), moBIE.getProject() ) );

		if ( moBIE.getDatasets() != null && moBIE.getDatasets().size() > 1 )
		{
			panel.add( new JSeparator( SwingConstants.HORIZONTAL ) );
			panel.add( createDatasetSelectionPanel() );
		}

		if ( moBIE.getViews() != null )
		{
			panel.add( new JSeparator( SwingConstants.HORIZONTAL ) );
			panel.add( createViewsSelectionPanel( moBIE.getViews() ) );
		}

		panel.add( createViewPanel() );
		panel.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		panel.add( createLocationPanel( moBIE.getDataset().getDefaultLocation() )  );
		panel.add( createClearAndSourceNamesOverlayPanel( moBIE ) );

		return panel;
	}

	public JPanel createImageDisplaySettingsPanel( ImageDisplay< ? > display )
	{
		JPanel panel = createDisplayPanel( display.getName() );

		// Set panel background color
		final Converter< ?, ARGBType > converter = display.sourceAndConverters().get( 0 ).getConverter();
		if ( converter instanceof ColorConverter )
		{
			setPanelColor( panel, ( ( ColorConverter ) converter ).getColor() );
		}

		List< ? extends SourceAndConverter< ? > > sourceAndConverters = display.sourceAndConverters();

		// Buttons
		panel.add( space() );
		panel.add( createFocusButton( display, display.sliceViewer.getBdvHandle(), sourceAndConverters.stream().map( sac -> sac.getSpimSource() ).collect( Collectors.toList() ) ) );
		panel.add( createContrastButton( sourceAndConverters, display.getName(), display.sliceViewer.getBdvHandle(), true ) );
		panel.add( createColorButton( panel, sourceAndConverters, display.sliceViewer.getBdvHandle() ) );
		//panel.add( createImageDisplayBrightnessButton( display ) );
		panel.add( createImageRenderingSettingsButton( sourceAndConverters, display.imageVolumeViewer ) );
		panel.add( createRemoveButton( display ) );
		// Checkboxes
		panel.add( space() );
		panel.add( createSliceViewerVisibilityCheckbox( display.isVisible(), sourceAndConverters ) );
		panel.add( createImageVolumeViewerVisibilityCheckbox( display ) );
		panel.add( createBVVVisibilityCheckbox( display, sourceAndConverters ) );
		panel.add( createCheckboxPlaceholder() ); // Table
		panel.add( createCheckboxPlaceholder() ); // Scatter plot

		// make the panel color listen to color changes of the sources
		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
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

		List< SourceAndConverter< ? > > sourceAndConverters = display.sourceAndConverters();

		panel.add( space() );
		panel.add( createFocusButton( display, display.sliceViewer.getBdvHandle(), sourceAndConverters.stream().map( sac -> sac.getSpimSource() ).collect( Collectors.toList() ) ) );
		panel.add( createContrastButton( sourceAndConverters, display.getName(), display.sliceViewer.getBdvHandle(), false ) );
		panel.add( createButtonPlaceholder() );
		panel.add( createSegmentsRenderingSettingsButton( sourceAndConverters, display.segmentVolumeViewer ) );
		panel.add( createRemoveButton( display ) );
		panel.add( space() );
		panel.add( createSliceViewerVisibilityCheckbox( display.isVisible(), sourceAndConverters ) );

		final AnnData annData = display.getAnnData();
		if ( annData != null ) // TODO: can annData ever be null??
		{
			// segments 3D view
			panel.add( createSegmentsVolumeViewerVisibilityCheckbox( display ) );
			// BVV view
			panel.add( createBVVVisibilityCheckbox( display, sourceAndConverters ) );
			// table view
			panel.add( createTableVisibilityCheckbox( display.tableView, display.showTable() ) );
			// scatter plot view
			panel.add( createScatterPlotViewerVisibilityCheckbox( display.scatterPlotView, display.showScatterPlot() ) );
		}
		else
		{
			panel.add( createCheckboxPlaceholder() );
			panel.add( createCheckboxPlaceholder() );
			panel.add( createCheckboxPlaceholder() );
			panel.add( createCheckboxPlaceholder() );
		}

		return panel;
	}

	private JButton createImageRenderingSettingsButton( List< ? extends SourceAndConverter< ? > > sourceAndConverters, ImageVolumeViewer imageVolumeViewer )
	{
		JButton button = getIconButton( "settings.png" );
		button.addActionListener( e ->
		{
			SwingUtilities.invokeLater( () ->
			{
				new Thread( () ->
				{
					final SourceAndConverter[] sacArray = sourceAndConverters.toArray( new SourceAndConverter[ 0 ] );
					Services.commandService.run( ConfigureImageRenderingCommand.class, true, "sourceAndConverters", sacArray, "volumeViewer", imageVolumeViewer );
				} ).start();
			} );
		} );
		return button;
	}
	
	private JButton createImageRenderingSettingsButton( List< ? extends SourceAndConverter< ? > > sourceAndConverters )
	{
		JButton button = getIconButton( "settings.png" );
		button.addActionListener( e ->
		{
			SwingUtilities.invokeLater( () ->
			{
				new Thread( () ->
				{
					final SourceAndConverter[] sacArray = sourceAndConverters.toArray( new SourceAndConverter[ 0 ] );
					Services.commandService.run( ConfigureImageRenderingCommand.class, true, "sourceAndConverters", sacArray);
				} ).start();
			} );
		} );
		return button;
	}

	private JButton createLabelRenderingSettingsButton( List< SourceAndConverter< ? > > sourceAndConverters )
	{
		JButton button = getIconButton( "settings.png" );
		button.setPreferredSize( PREFERRED_BUTTON_SIZE );
		button.addActionListener( e -> Services.commandService.run( ConfigureLabelRenderingCommand.class, true, "sourceAndConverters", sourceAndConverters.toArray( new SourceAndConverter[ 0 ] ) ) );
		return button;
	}

	private JButton createSegmentsRenderingSettingsButton( List< SourceAndConverter< ? > > sourceAndConverters, SegmentVolumeViewer< ? > segmentVolumeViewer )
	{
		JButton button = getIconButton( "settings.png" );
		button.setPreferredSize( PREFERRED_BUTTON_SIZE );
		button.addActionListener( e -> Services.commandService.run( ConfigureSegmentRenderingCommand.class, true, "sourceAndConverters", sourceAndConverters.toArray( new SourceAndConverter[ 0 ] ), "volumeViewer", segmentVolumeViewer ) );
		return button;
	}

	private JButton createSpotSettingsButton( List< SourceAndConverter< ? > > sourceAndConverters )
	{
		JButton button = getIconButton( "settings.png" );
		button.setPreferredSize( PREFERRED_BUTTON_SIZE );
		button.addActionListener( e ->
		{
			final SourceAndConverter[] sacArray = sourceAndConverters.toArray( new SourceAndConverter[ 0 ] );
			Services.commandService.run( ConfigureSpotRenderingCommand.class, true, "sourceAndConverters", sacArray );
		} );
		return button;
	}

	public JPanel createViewsSelectionPanel( Map< String, View > views )
	{
		groupingsToViews = new HashMap<>(  );
		groupingsToComboBox = new HashMap<>( );
		groupingsToPanels = new HashMap<>();
		viewSelectionPanel = new JPanel( new BorderLayout() );
		viewSelectionPanel.setLayout( new BoxLayout( viewSelectionPanel, BoxLayout.Y_AXIS ) );

		addViewsToViewSelectionPanel( views );

		return viewSelectionPanel;
	}

	public void addViewsToViewSelectionPanel( Map< String, View > views )
	{
		for ( String viewName : views.keySet() )
		{
			final View view = views.get( viewName );
			Set< String > uiSelectionGroups = view.getUiSelectionGroups();

			for ( String uiSelectionGroup : uiSelectionGroups )
			{
				if ( ! groupingsToViews.containsKey( uiSelectionGroup ) )
					groupingsToViews.put( uiSelectionGroup, new LinkedHashMap<>( ));
				groupingsToViews.get( uiSelectionGroup ).put( viewName, view );
			}
		}

		final ArrayList< String > uiSelectionGroups = new ArrayList<>( groupingsToViews.keySet() );
		// sort in alphabetical order, ignoring upper/lower case
		uiSelectionGroups.sort( new Comparator< String >()
        {
            @Override
            public int compare( String s1, String s2 )
            {
                return s1.compareToIgnoreCase( s2 );
            }
        } );

		// If it's the first time, add all the view selection panels in order
		if ( groupingsToComboBox.keySet().isEmpty() ) {
			for (String uiSelectionGroup : uiSelectionGroups) {
				final JPanel selectionPanel = createViewSelectionPanel(moBIE, uiSelectionGroup, groupingsToViews.get(uiSelectionGroup));
				viewSelectionPanel.add(selectionPanel);
			}

			refreshViewsSelectionPanelHeight();
			return;
		}

		// If there are already panels, add new ones at the correct index to maintain alphabetical order
		Map< Integer, JPanel > indexToPanel = new HashMap<>();
		for ( String viewName : views.keySet() ) {
			Set< String > groups = views.get( viewName ).getUiSelectionGroups();
			for ( String group : groups )
			{
				if ( groupingsToComboBox.containsKey( group ) )
				{
					JComboBox< String > comboBox = groupingsToComboBox.get( group );
					// check if a view of that name already exists: -1 means it doesn't exist
					int index = ( ( DefaultComboBoxModel ) comboBox.getModel() ).getIndexOf( viewName );
					if ( index == -1 )
					{
						comboBox.addItem( viewName );
					}
				}
				else
				{
					final JPanel selectionPanel = createViewSelectionPanel( moBIE, group, groupingsToViews.get( group ) );
					int alphabeticalIndex = uiSelectionGroups.indexOf( group );
					indexToPanel.put( alphabeticalIndex, selectionPanel );
				}
			}
		}

		if ( ! indexToPanel.keySet().isEmpty() ) {
			// add panels in ascending index order
			final ArrayList<Integer> sortedIndices = new ArrayList<>(indexToPanel.keySet());
			Collections.sort(sortedIndices);
			for (Integer index : sortedIndices) {
				viewSelectionPanel.add(indexToPanel.get(index), index.intValue());
			}
		}
		refreshViewsSelectionPanelHeight();
	}

	public void removeViewsFromViewSelectionPanel( Map< String, View > views )
	{
		for ( String viewName : views.keySet() )
		{
			Set< String > groups = views.get( viewName ).getUiSelectionGroups();
			for ( String group : groups )
			{
				if ( groupingsToViews.containsKey( group ) )
				{
					Map< String, View > groupViews = groupingsToViews.get( group );
					groupViews.remove( viewName );

					if ( groupViews.isEmpty() )
					{
						groupingsToViews.remove( group );
					}
				}

				if ( groupingsToComboBox.containsKey( group ) )
				{
					JComboBox< String > comboBox = groupingsToComboBox.get( group );
					comboBox.removeItem( viewName );

					if ( comboBox.getItemCount() == 0 )
					{
						groupingsToComboBox.remove( group );
						viewSelectionPanel.remove( groupingsToPanels.get( group ) );
						groupingsToPanels.remove( group );
					}
				}
			}
		}

		refreshViewsSelectionPanelHeight();
	}

	private void refreshViewsSelectionPanelHeight() {
		viewsSelectionPanelHeight = groupingsToViews.keySet().size() * 40;
	}

	public Map< String, Map< String, View > > getGroupingsToViews()
	{
		return groupingsToViews;
	}

	public int getSelectionPanelHeight()
	{
		return viewsSelectionPanelHeight + 5 * 40;
	}

	public Set<String> getGroupings() {
		return groupingsToViews.keySet();
	}

	private JPanel createClearAndSourceNamesOverlayPanel( MoBIE moBIE )
	{
		final JPanel panel = SwingHelper.horizontalBoxLayoutPanel();

		final JButton button = SwingHelper.createButton( "clear", new Dimension( 80, SwingHelper.TEXT_FIELD_HEIGHT ) );
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

		overlayNamesCheckbox = new JCheckBox( "overlay image names" );
		overlayNamesCheckbox.setSelected( false );
		overlayNamesCheckbox.addActionListener( e -> new Thread( () ->
				moBIE.getViewManager().getSliceViewer()
						.getImageNameOverlay().setActive( overlayNamesCheckbox.isSelected() ) ).start() );

		panel.add( overlayNamesCheckbox );
		panel.add( space() );
		panel.add( button );
		return panel;
	}

	private JPanel createViewSelectionPanel( MoBIE moBIE, String panelName, Map< String, View > views )
	{
		final JPanel horizontalLayoutPanel = SwingHelper.horizontalBoxLayoutPanel();

		final JComboBox< String > comboBox = new JComboBox<>( views.keySet().toArray( new String[ 0 ] ) );

		final JButton button = SwingHelper.createButton( VIEW );
		button.addActionListener( e ->
		{
			new Thread( () -> {
				final String viewName = ( String ) comboBox.getSelectedItem();
				final View view = views.get( viewName );
				view.setName( viewName );
				moBIE.getViewManager().show( view );
			} ).start();
		} );

		SwingHelper.setComboBoxDimensions( comboBox, PROTOTYPE_DISPLAY_VALUE );

		horizontalLayoutPanel.add( SwingHelper.getJLabel( panelName ) );
		horizontalLayoutPanel.add( comboBox );
		horizontalLayoutPanel.add( button );

		groupingsToComboBox.put( panelName, comboBox );
		groupingsToPanels.put( panelName, horizontalLayoutPanel );

		return horizontalLayoutPanel;
	}

	public JPanel createViewPanel( )
	{
		final JPanel panel = SwingHelper.horizontalBoxLayoutPanel();
		final JButton button = SwingHelper.createButton( VIEW );
		final JTextField jTextField = new JTextField( "" );
		jTextField.setPreferredSize( new Dimension( SwingHelper.COMBOBOX_WIDTH, SwingHelper.TEXT_FIELD_HEIGHT ) );
		jTextField.setMinimumSize( new Dimension( SwingHelper.COMBOBOX_WIDTH, SwingHelper.TEXT_FIELD_HEIGHT ) );
		jTextField.setMaximumSize( new Dimension( Integer.MAX_VALUE, SwingHelper.TEXT_FIELD_HEIGHT ) );

		JPopupMenu suggestionsPopup = new JPopupMenu();

		Set< String > views = this.moBIE.getViews().keySet();
		jTextField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate( DocumentEvent e) {
				showSuggestions();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				showSuggestions();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				showSuggestions();
			}

			private void showSuggestions() {
				String input = jTextField.getText();
				suggestionsPopup.removeAll();

				if (!input.isEmpty()) {
					for (String suggestion : views) {
						if (suggestion.startsWith(input)) {
							JMenuItem item = new JMenuItem(suggestion);
							item.addActionListener(new ActionListener() {
								@Override
								public void actionPerformed(ActionEvent e) {
									jTextField.setText(suggestion);
									suggestionsPopup.setVisible(false);
								}
							});
							suggestionsPopup.add(item);
						}
					}

					if (suggestionsPopup.getComponentCount() > 0) {
						suggestionsPopup.show(jTextField, 0, jTextField.getHeight());
					} else {
						suggestionsPopup.setVisible(false);
					}
				} else {
					suggestionsPopup.setVisible(false);
				}
			}
		});

		button.addActionListener( e ->
		{
			String text = jTextField.getText();
			if ( views.contains( text ) )
			{
				this.moBIE.getViewManager().show( text );
			}
			else
			{
				IJ.log( "[WARNING] View " + text + " not found." );
			}
		} );

		JLabel label = SwingHelper.getJLabel( "enter view" );
		label.setToolTipText( "Views can be selected from the above drop-downs.\n" +
				"Alternatively, a view can also be directly chosen here." );
		panel.add( label );
		panel.add( jTextField );
		panel.add( button );

		return panel;
	}

	public JPanel createLocationPanel( ViewerTransform transform )
	{
		final JPanel panel = SwingHelper.horizontalBoxLayoutPanel();
		final JButton button = SwingHelper.createButton( MOVE );
		final JTextField jTextField = new JTextField( ViewerTransform.toString( transform ) );
		jTextField.setPreferredSize( new Dimension( SwingHelper.COMBOBOX_WIDTH, SwingHelper.TEXT_FIELD_HEIGHT ) );
		jTextField.setMinimumSize( new Dimension( SwingHelper.COMBOBOX_WIDTH, SwingHelper.TEXT_FIELD_HEIGHT ) );
		jTextField.setMaximumSize( new Dimension( Integer.MAX_VALUE, SwingHelper.TEXT_FIELD_HEIGHT ) );

		JPopupMenu suggestionsPopup = new JPopupMenu();

		Set< String > views = this.moBIE.getViews().keySet();
		jTextField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate( DocumentEvent e) {
				showSuggestions();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				showSuggestions();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				showSuggestions();
			}

			private void showSuggestions() {
				String input = jTextField.getText();
				suggestionsPopup.removeAll();

				if (!input.isEmpty()) {
					for (String suggestion : views) {
						if (suggestion.startsWith(input)) {
							JMenuItem item = new JMenuItem(suggestion);
							item.addActionListener(new ActionListener() {
								@Override
								public void actionPerformed(ActionEvent e) {
									jTextField.setText(suggestion);
									suggestionsPopup.setVisible(false);
								}
							});
							suggestionsPopup.add(item);
						}
					}

					if (suggestionsPopup.getComponentCount() > 0) {
						suggestionsPopup.show(jTextField, 0, jTextField.getHeight());
					} else {
						suggestionsPopup.setVisible(false);
					}
				} else {
					suggestionsPopup.setVisible(false);
				}
			}
		});

		button.addActionListener( e ->
		{
			String text = jTextField.getText();
			if ( views.contains( text ) )
			{
				this.moBIE.getViewManager().show( text );
			}
			else
			{
				ViewerTransform viewerTransform = ViewerTransform.toViewerTransform( text );
				ViewerTransformChanger.apply( this.moBIE.getViewManager().getSliceViewer().getBdvHandle(), viewerTransform );
			}
		} );

		JLabel label = SwingHelper.getJLabel( "location" );
		label.setToolTipText( "<html>Change the location (i.e. viewer transformation)\n of the current view.<br>" +
				"Right click in BigDataViewer and choose \"Log Current Location\" to obtain valid entries.<br>" +
				"See also https://mobie.github.io/tutorials/views_and_locations.html</html>" );
		panel.add( label );
		panel.add( jTextField );
		panel.add( button );

		return panel;
	}

	public JPanel createInfoPanel( String projectLocation, Project project )
	{
		final JPanel panel = SwingHelper.horizontalBoxLayoutPanel();

		final JButton button = SwingHelper.createButton( HELP );

		final MoBIEInfo moBIEInfo = new MoBIEInfo( projectLocation, project );

		final JComboBox< String > comboBox = new JComboBox<>( moBIEInfo.getInfoChoices() );
		SwingHelper.setComboBoxDimensions( comboBox, PROTOTYPE_DISPLAY_VALUE );

		button.addActionListener( e -> {
			moBIEInfo.showInfo( ( String ) comboBox.getSelectedItem() );
		} );
		comboBox.setPrototypeDisplayValue( PROTOTYPE_DISPLAY_VALUE  );

		panel.setSize( 0, 80 );
		final ImageIcon icon = createIcon( 80 );
		final JLabel moBIE = new JLabel( "                   " );
		moBIE.setIcon( icon );

		panel.add( moBIE );
		panel.add( comboBox );
		panel.add( button );

		return panel;
	}

	public ImageIcon createIcon( int size )
	{
		final URL resource = UserInterfaceHelper.class.getResource( "/mobie.png" );
		final ImageIcon imageIcon = new ImageIcon( resource );
		final Image scaledInstance = imageIcon.getImage().getScaledInstance( size, size, Image.SCALE_SMOOTH );
		return new ImageIcon( scaledInstance );
	}

	public JPanel createDatasetSelectionPanel( )
	{
		final JPanel panel = SwingHelper.horizontalBoxLayoutPanel();

		final JComboBox< String > comboBox = new JComboBox<>( moBIE.getDatasets().toArray( new String[ 0 ] ) );

		final JButton button = SwingHelper.createButton( VIEW );
		button.addActionListener( e ->
		{
			SwingUtilities.invokeLater( () ->
			{
				final String dataset = ( String ) comboBox.getSelectedItem();
				moBIE.setDataset( dataset );
			} );
		} );

		comboBox.setSelectedItem( moBIE.getDataset().getName() );
		SwingHelper.setComboBoxDimensions( comboBox, PROTOTYPE_DISPLAY_VALUE );

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
		checkBox.setToolTipText( "Toggle dataset visibility" );
		checkBox.setSelected( display.showSelectedSegmentsIn3d() );
		checkBox.setPreferredSize( PREFERRED_CHECKBOX_SIZE );

		checkBox.addActionListener( e -> new Thread( () ->
		{
				display.segmentVolumeViewer.showSegments( checkBox.isSelected(), false );
		}).start() );

		display.segmentVolumeViewer.getListeners().add( new VisibilityListener()
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
			final List< ? extends SourceAndConverter< ? > > sourceAndConverters )
	{
		JCheckBox checkBox = new JCheckBox( "S" );
		checkBox.setToolTipText( "Toggle slice viewer visibility" );
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

	private static Component createTableVisibilityCheckbox(
			TableView tableView,
			boolean isVisible )
	{
		if ( tableView == null )
			return createCheckboxPlaceholder();

		JCheckBox checkBox = new JCheckBox( "T" );
		checkBox.setToolTipText( "Toggle table visibility" );
		checkBox.setSelected( isVisible );
		checkBox.setPreferredSize( PREFERRED_CHECKBOX_SIZE );
		try
		{
			tableView.setVisible( isVisible );
		}
		catch ( Exception e )
		{
			IJ.log( "Error making visible " + tableView );
		}
		checkBox.addActionListener( e -> SwingUtilities.invokeLater( () -> tableView.setVisible( checkBox.isSelected() ) ) );
		tableView.getWindow().addWindowListener(
				new WindowAdapter() {
					public void windowClosing( WindowEvent ev) {
						checkBox.setSelected( false );
					}
		});

		return checkBox;
	}

	private static Component createScatterPlotViewerVisibilityCheckbox(
			ScatterPlotView< ? > scatterPlotView,
			boolean isVisible )
	{
		if ( scatterPlotView == null )
			return createCheckboxPlaceholder();

		JCheckBox checkBox = new JCheckBox( "P" );
		checkBox.setToolTipText( "Toggle scatter plot visibility" );
		checkBox.setSelected( isVisible );
		checkBox.setPreferredSize( PREFERRED_CHECKBOX_SIZE );
		checkBox.addActionListener( e ->
			SwingUtilities.invokeLater( () ->
				{
					if ( checkBox.isSelected() )
					{
						boolean show = scatterPlotView.show( true );
						checkBox.setSelected( show );
					}
					else
					{
						scatterPlotView.hide();
					}
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

	public static JCheckBox createBVVVisibilityCheckbox(
			AbstractDisplay< ? > display,
			final List< ? extends SourceAndConverter< ? > > sourceAndConverters  )
	{
		JCheckBox checkBox = new JCheckBox( "B" );
		checkBox.setToolTipText( "Toggle dataset visibility" );
		checkBox.setSelected( false );
		checkBox.setPreferredSize( PREFERRED_CHECKBOX_SIZE );

		checkBox.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				new Thread( () -> {
					for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
					{
						display.bigVolumeViewer.showSource( sourceAndConverter, checkBox.isSelected() );
					}
				}).start();
			}
		} );

		display.bigVolumeViewer.getListeners().add( new VisibilityListener()
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
		checkBox.setToolTipText( "Toggle dataset visibility" );
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


	public static JButton createFocusButton( AbstractDisplay< ? > sourceDisplay,
											 BdvHandle bdvHandle,
											 List< Source< ? > > sources )
	{
		JButton button = getIconButton( "focus.png" );
		button.setToolTipText( "Fit image to viewer" );

		button.addActionListener( e ->
		{
			if ( sources.size() == 1 )
			{
				final AffineTransform3D transform =
						MoBIEViewerTransformAdjuster.getViewerTransform(
								sourceDisplay.sliceViewer.getBdvHandle(),
								sources.get( 0 )
						);
				ViewerTransformChanger.apply( bdvHandle, transform, ViewerTransformChanger.animationDurationMillis );
			}
			else
			{
				final AffineTransform3D transform =
						MoBIEViewerTransformAdjuster.getViewerTransform(
							sourceDisplay.sliceViewer.getBdvHandle(),
							sources );

				ViewerTransformChanger.apply( bdvHandle, transform, ViewerTransformChanger.animationDurationMillis );
			}
		} );

		return button;
	}

	public static JButton createContrastButton(
			final List< ? extends SourceAndConverter< ? > > sacs,
			final String name,
			final BdvHandle bdvHandle,
			final boolean addContrastLimitUI )
	{
		JButton button = getIconButton( "contrast.png" );
		button.setToolTipText( "Change opacity and contrast limits" );

		button.addActionListener( e ->
		{
			JFrame jFrame = ContrastAdjustmentsDialog.showDialog(
					name,
					sacs, // sacs,
					bdvHandle,
					addContrastLimitUI
			);

			MoBIEWindowManager.addWindow( jFrame );
		} );

		return button;
	}

	@NotNull
	private static JButton getIconButton( final String iconResource )
	{
		final URL resource = UserInterfaceHelper.class.getResource( "/" + iconResource );
		Image scaledInstance = new ImageIcon( resource ).getImage().getScaledInstance( 19, 19, Image.SCALE_SMOOTH );
		ImageIcon imageIcon = new ImageIcon( scaledInstance );
		JButton button = new JButton( imageIcon );
		button.setPreferredSize( PREFERRED_BUTTON_SIZE);
		return button;
	}

	private static JButton createColorButton( JPanel parentPanel, List< ? extends SourceAndConverter< ? > > sourceAndConverters, BdvHandle bdvHandle )
	{
		JButton button = getIconButton( "color.png" );
		button.setToolTipText( "Change color" );

		button.addActionListener( e ->
		{
			Color color = JColorChooser.showDialog( null, "", null );
			if ( color == null ) return;

			parentPanel.setBackground( color );

			for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
			{
				new ColorChanger( sourceAndConverter, ColorHelper.getARGBType( color ) ).run();
			}

			bdvHandle.getViewerPanel().requestRepaint();
		} );

		return button;
	}

	private void setPanelColor( JPanel panel, ARGBType argbType )
	{
		final Color color = ColorHelper.getColor( argbType );
		if ( color != null )
		{
			panel.setOpaque( true );
			panel.setBackground( color );
		}
	}

	private void setPanelColor( JPanel panel, String colorString )
	{
		final Color color = ColorHelper.getColor( colorString );
		if ( color != null )
		{
			panel.setOpaque( true );
			panel.setBackground( color );
		}
	}

	private JButton createRemoveButton( Display display )
	{
		JButton button = getIconButton( "delete.png" );
		button.setToolTipText( "Remove layer" );

		button.addActionListener( e ->
		{
			// remove the display but do not close the ImgLoader
			// because some derived sources may currently be shown
			moBIE.getViewManager().removeDisplay( display, false );
		} );

		return button;
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

	public static void saveTableUI( JTable table )
	{
		final JFileChooser jFileChooser = new JFileChooser( "" );

		if ( jFileChooser.showSaveDialog( null ) == JFileChooser.APPROVE_OPTION )
		{
			final File selectedFile = jFileChooser.getSelectedFile();

			saveTable( table, selectedFile );
		}
	}

	public static void saveTable( JTable table, File tableOutputFile )
	{
		try
		{
			saveTableWithIOException( table, tableOutputFile );
		} catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	public static void saveColumns( JTable table )
	{
		final ArrayList< String > selectedColumns = selectColumnNamesUI( table, "Select columns" );

		final JTable newTable = createNewTableFromSelectedColumns( table, selectedColumns );

		final JFileChooser jFileChooser = new JFileChooser( "" );

		if ( jFileChooser.showSaveDialog( null ) == JFileChooser.APPROVE_OPTION )
		{
			final File selectedFile = jFileChooser.getSelectedFile();

			saveTable( newTable, selectedFile );
		}
	}

	public static JTable createNewTableFromSelectedColumns( JTable table, ArrayList< String > selectedColumns )
	{
		DefaultTableModel newModel = new DefaultTableModel();
		final TableModel model = table.getModel();
		final int rowCount = table.getRowCount();

		for ( String columnName : selectedColumns )
		{
			int viewIndex = table.getColumnModel().getColumnIndex( columnName );
			int modelIndex = table.convertColumnIndexToModel( viewIndex );
			final Object[] objects = new Object[ rowCount ];
			for ( int rowIndex = 0; rowIndex < objects.length; rowIndex++ )
				objects[ rowIndex ] = model.getValueAt( rowIndex, modelIndex );

			newModel.addColumn( columnName, objects );
		}

		return new JTable( newModel );
	}

	public static String[] getColumnNamesAsArray( JTable jTable )
	{
		final String[] columnNames = new String[ jTable.getColumnCount() ];

		for ( int columnIndex = 0; columnIndex < jTable.getColumnCount(); columnIndex++ )
		{
			columnNames[ columnIndex ] = jTable.getColumnName( columnIndex );
		}
		return columnNames;
	}

	public static ArrayList< String > selectColumnNamesUI( JTable table, String text )
	{
		final String[] columnNames = getColumnNamesAsArray( table );
		final int n = (int) Math.ceil( Math.sqrt( columnNames.length ) );
		final GenericDialog gd = new GenericDialog( "" );
		boolean[] booleans = new boolean[ columnNames.length ];
		gd.addCheckboxGroup( n, n, columnNames, booleans );
		gd.showDialog();
		if ( gd.wasCanceled() ) return null;

		final ArrayList< String > selectedColumns = new ArrayList<>();
		for ( int i = 0; i < columnNames.length; i++ )
			if ( gd.getNextBoolean() )
				selectedColumns.add( columnNames[ i ] );

		return selectedColumns;
	}

	private static void saveTableWithIOException( JTable table, File file ) throws IOException
	{
		BufferedWriter bfw = new BufferedWriter( new FileWriter( file ) );

		final int lastColumn = table.getColumnCount() - 1;

		// header
		for ( int col = 0; col < lastColumn; col++ )
			bfw.write( table.getColumnName( col ) + "\t" );
		bfw.write( table.getColumnName( lastColumn ) + "\n" );

		// content
		for ( int row = 0; row < table.getRowCount(); row++ )
		{
			for ( int col = 0; col < lastColumn; col++ )
				bfw.write( table.getValueAt( row, col ) + "\t" );
			bfw.write( table.getValueAt( row, lastColumn ) + "\n" );
		}

		bfw.close();
	}

}
