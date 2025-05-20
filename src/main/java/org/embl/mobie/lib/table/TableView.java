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
package org.embl.mobie.lib.table;

import ij.IJ;
import ij.gui.GenericDialog;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.annotation.AnnotatedRegion;
import org.embl.mobie.lib.annotation.AnnotationUI;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.bdv.overlay.AnnotatedRegionsOverlay;
import org.embl.mobie.lib.bdv.overlay.AnnotatedSegmentsOrSpotsOverlay;
import org.embl.mobie.lib.bdv.overlay.AnnotationOverlay;
import org.embl.mobie.lib.bdv.view.SliceViewer;
import org.embl.mobie.lib.color.CategoricalAnnotationColoringModel;
import org.embl.mobie.lib.color.ColorHelper;
import org.embl.mobie.lib.color.ColoringListener;
import org.embl.mobie.lib.color.ColoringModel;
import org.embl.mobie.lib.color.MobieColoringModel;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.plot.ScatterPlotSettings;
import org.embl.mobie.lib.serialize.display.AbstractAnnotationDisplay;
import org.embl.mobie.lib.plot.ScatterPlotView;
import org.embl.mobie.lib.select.SelectionListener;
import org.embl.mobie.lib.select.SelectionModel;
import org.embl.mobie.lib.serialize.display.RegionDisplay;
import org.embl.mobie.lib.table.columns.ColumnNames;
import org.embl.mobie.ui.*;
import net.imglib2.type.numeric.ARGBType;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.embl.mobie.lib.io.FileLocation;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import static org.embl.mobie.ui.UserInterfaceHelper.loadFromProjectOrFileSystemDialog;

public class TableView< A extends Annotation > implements SelectionListener< A >, ColoringListener, AnnotationListener< A >
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	private final AnnotationTableModel< A > tableModel;
	private final SelectionModel< A > selectionModel;
	private final MobieColoringModel< A > coloringModel;
	private final String tableName;
	private final SliceViewer sliceViewer;
	private final AbstractAnnotationDisplay< A > display;
	private JTable jTable;
	private int recentlySelectedRowInView;
	private RowSelectionMode selectionMode = RowSelectionMode.FocusOnly;
	private JFrame frame;
	private SwingTableModel swingTableModel;
	private boolean controlKeyPressed;
	private boolean doubleClick;
	private AnnotationOverlay annotationOverlay;

	private enum RowSelectionMode
	{
		None,
		FocusOnly,
		ToggleSelectionAndFocusIfSelected
	}

	public TableView( AbstractAnnotationDisplay< A > display )
	{
		this.display = display;
		this.tableModel = display.getAnnData().getTable();
		this.coloringModel = display.coloringModel;
		this.selectionModel = display.selectionModel;
		this.tableName = display.getName();
		this.recentlySelectedRowInView = -1;

		// TODO: conceptually does not feel correct
		//  that the TableView would need
		//  to know about the sliceViewer, but we need this reference
		//  in order to overlay annotations, and implementing
		//  some other (better) way feels like too much work right now
		this.sliceViewer = display.sliceViewer;

		tableModel.addAnnotationListener( this );
		configureJTable();
		installSelectionModelNotification();
		configureRowColoring();
	}

	public void close()
	{
		frame.dispose();
		if ( annotationOverlay != null ) annotationOverlay.close();
	}

	public void show()
	{
		// Prefetch columns and wait a bit as this hopefully makes it less likely that
		// there are errors thrown by Java Swing when rendering the table window
		// below during frame.pack()
		TableColumnModel columnModel = jTable.getColumnModel();
		int columnCount = columnModel.getColumnCount();
		for ( int columnIndex = 0; columnIndex < columnCount; columnIndex++ )
		{
			TableColumn column = columnModel.getColumn( columnIndex );
			if ( column == null )
				throw new NullPointerException("");
		}
		IJ.log( "Showing table \"" + tableName + "\" with " + columnCount + " columns.");
		IJ.wait( 200 );

		final JPanel panel = new JPanel( new GridLayout( 1, 0 ) );
		JScrollPane scrollPane = new JScrollPane(
				jTable,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		panel.add( scrollPane );

		panel.updateUI();
		panel.setOpaque( true );

		frame = new JFrame( tableName );
		final JMenuBar menuBar = createMenuBar();
		frame.setJMenuBar( menuBar );
		frame.setContentPane( panel );

		try
		{
			// Adding a delay
			Timer timer = new Timer(200, e -> {
				frame.pack();
				frame.setVisible(true);
			});
			timer.setRepeats(false);
			timer.start();
		}
		catch ( Exception e )
		{
			IJ.log("Error showing table: " + tableName );
		}

		frame.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
		frame.addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent ev) {
				frame.setVisible( false );
			}
		});	//frame.pack();

		if ( display instanceof RegionDisplay )
		{
			// Show a default AnnotationOverlay

			if ( annotationOverlay != null )
				annotationOverlay.close();

			annotationOverlay = new AnnotatedRegionsOverlay(
					sliceViewer,
					tableModel.annotations(),
					ColumnNames.REGION_ID,
					-1
			);
		}
	}

	private void configureJTable()
	{
		swingTableModel = new SwingTableModel( tableModel );
		jTable = new JTable( swingTableModel );
		jTable.updateUI();
		jTable.setPreferredScrollableViewportSize( new Dimension( 500, 200 ) );
		jTable.setFillsViewportHeight( true );
		jTable.setAutoCreateRowSorter( true );
		jTable.setRowSelectionAllowed( true );
		jTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		jTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
	}

	public String getName()
	{
		return tableName;
	}

	private JMenuBar createMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		menuBar.add( createTableMenu() );

		if ( selectionModel != null )
			menuBar.add( createSelectionMenu() );

		if ( coloringModel != null )
		{
			menuBar.add( createColoringMenu() );
			menuBar.add( createAnnotateMenu() );
			// menuBar.add( createPlotMenu() ); we have this already in the MoBIE UI
		}

		menuBar.add( createComputeMenu() );

		menuBar.add( createMiscMenu() );

		return menuBar;
	}

	private JMenu createSelectionMenu()
	{
		JMenu menu = new JMenu( "Select" );
		menu.add( createSelectAllMenuItem() );
		menu.add( createSelectEqualToMenuItem() );
		menu.add( createSelectLessThanMenuItem() );
		menu.add( createSelectGreaterThanMenuItem() );
		return menu;
	}

	private JMenu createMiscMenu()
	{
		JMenu menu = new JMenu( "Misc" );
		menu.add( createColumnSearchMenuItem() );
		return menu;
	}

	private JMenu createComputeMenu()
	{
		JMenu menu = new JMenu( "Analyse" );
		menu.add( createComputeDistanceMenuItem() );
		return menu;
	}

	private JMenu createAnnotateMenu()
	{
		JMenu menu = new JMenu( "Annotations" );
		menu.add( startNewAnnotationMenuItem() );
		menu.add( continueAnnotationMenuItem() );
		menu.add( overlayAnnotationMenuItem() );
		menu.add( removeAnnotationOverlayMenuItem() );

		return menu;
	}

	private JMenu createPlotMenu()
	{
		JMenu menu = new JMenu( "Plot" );
		menu.add( createScatterPlotMenuItem() );
		return menu;
	}

	private JMenuItem createScatterPlotMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Scatter Plot..." );
		menuItem.addActionListener( e ->
			{
				SwingUtilities.invokeLater( () ->
				{
					List< String > columnNames = tableModel.columnNames();
					final ScatterPlotSettings settings = new ScatterPlotSettings( new String[]{ columnNames.get( 0 ), columnNames.get( 1 ) } );
					ScatterPlotDialog dialog = new ScatterPlotDialog( columnNames, settings );
					if ( dialog.show() )
					{
						ScatterPlotView< A > scatterPlot = new ScatterPlotView<>( tableModel, selectionModel, coloringModel, dialog.getSettings() );
						scatterPlot.show( false );
					}
				});
			}
		);
		return menuItem;
	}

	private JMenu createTableMenu()
    {
        JMenu menu = new JMenu( "Table" );
        menu.add( createSaveTableAsMenuItem() );
		menu.add( createSaveColumnsAsMenuItem() );
		menu.add( createLoadColumnsMenuItem() );
		menu.add( createAddStringColumnMenuItem() );
		return menu;
    }

	public void enableRowSorting( boolean sortable )
	{
		final int columnCount = jTable.getColumnCount();
		for ( int i = 0; i < columnCount; i++ )
		{
			((DefaultRowSorter) jTable.getRowSorter()).setSortable( i, sortable );
		}
	}

	private JMenuItem createAddStringColumnMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Add Text Column..." );
		menuItem.addActionListener( e ->
				new Thread( this::showAddStringColumnDialog ).start()
		);
		return menuItem;
	}

	private JMenuItem createLoadColumnsMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Load Columns..." );
		menuItem.addActionListener( e ->
			new Thread( () -> {
				FileLocation fileLocation = loadFromProjectOrFileSystemDialog();

				if ( fileLocation.equals( FileLocation.CurrentProject ) )
				{
					final String[] availableChunks = tableModel.getAvailableTableChunks().toArray( new String[ 0 ] );
					final GenericDialog gd = new GenericDialog("Choose table chunk");
					gd.addChoice("Chunks", availableChunks, availableChunks[0]);
					gd.showDialog();
					if ( gd.wasCanceled() ) return;
					String chunk = gd.getNextChoice();
					IJ.log( "Loading table chunk: " + chunk + "..." );
					tableModel.loadTableChunk( chunk );
				}
				else if ( fileLocation.equals( FileLocation.ExternalFile ) )
				{
					String path = UserInterfaceHelper.selectFilePath( "tsv", "Table", true );
					if ( path == null )  return;
					IJ.log( "Loading table chunk: " + path + "..." );
					final StorageLocation storageLocation = new StorageLocation();
					storageLocation.absolutePath = IOHelper.getParentLocation( path );
					storageLocation.defaultChunk = IOHelper.getFileName( path );
					tableModel.loadExternalTableChunk( storageLocation );
				}

				IJ.log( "...done!" );

			}).start()
		);
		return menuItem;
	}

	private synchronized void updateTable()
	{
		if ( jTable == null ) return;

		// https://github.com/mobie/mobie-viewer-fiji/issues/1146
		swingTableModel.tableChanged();

		if ( jTable.isVisible() )
			repaintTable();
	}

	private JMenuItem createSaveTableAsMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Save Table As..." );
		menuItem.addActionListener( e ->
				SwingUtilities.invokeLater( () ->
						UserInterfaceHelper.saveTableUI( jTable ) ) );
		return menuItem;
	}

	private JMenuItem createSaveColumnsAsMenuItem()
	{
		// FIXME: Copy the TableUIs code from ij-utils here
		final JMenuItem menuItem = new JMenuItem( "Save Columns As..." );
		menuItem.addActionListener( e ->
				SwingUtilities.invokeLater( () -> UserInterfaceHelper.saveColumns( jTable ) ) );
		return menuItem;
	}

	private JMenuItem createSelectAllMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Select All" );
		menuItem.addActionListener( e ->
				SwingUtilities.invokeLater( () ->
						selectionModel.setSelected( tableModel.annotations(), true ) ) );
		return menuItem;
	}

	private JMenuItem createColumnSearchMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Focus Column..." );
		menuItem.addActionListener( e ->
				SwingUtilities.invokeLater( () ->
				{
					StringArraySelectorDialog dialog =
							new StringArraySelectorDialog(
									"Column Selector",
									UserInterfaceHelper.getColumnNamesAsArray( jTable )
							);
					if ( ! dialog.show() ) return;
					final String columnName = dialog.getSelectedItem();
					int columnIndex = jTable.getColumnModel().getColumnIndex( columnName );
					JViewport viewport = (JViewport) jTable.getParent();
					Rectangle rect = jTable.getCellRect(0, columnIndex, true);
					Point pt = viewport.getViewPosition();
					rect.setLocation(rect.x - pt.x, rect.y - pt.y);
					viewport.scrollRectToVisible(rect);
				}) );
		return menuItem;
	}

	private JMenuItem createComputeDistanceMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Compute Distance to Selected Rows..." );
		menuItem.addActionListener( e ->
				SwingUtilities.invokeLater( ()
						-> DistanceComputer.showUI( tableModel, selectionModel, coloringModel ) ) );
		return menuItem;
	}

	private JMenuItem createSelectEqualToMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Select Equal To..." );
		menuItem.addActionListener( e ->
				SwingUtilities.invokeLater( this::selectEqualTo ) );
		return menuItem;
	}

	private JMenuItem createSelectLessThanMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Select Less Than..." );
		menuItem.addActionListener( e ->
				SwingUtilities.invokeLater( () ->
						selectGreaterOrLessThan( false ) ) );
		return menuItem;
	}

	private JMenuItem createSelectGreaterThanMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Select Greater Than..." );

		menuItem.addActionListener( e ->
				SwingUtilities.invokeLater( () ->
						selectGreaterOrLessThan( true )) );

		return menuItem;
	}

	private JMenuItem startNewAnnotationMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Start New Annotation..." );

		menuItem.addActionListener( e -> showNewAnnotationDialog() );

		return menuItem;
	}

	private JMenuItem continueAnnotationMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Continue Annotation..." );

		menuItem.addActionListener( e -> showContinueAnnotationDialog() );

		return menuItem;
	}

	private JMenuItem overlayAnnotationMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Overlay Annotation..." );

		menuItem.addActionListener( e -> overlayAnnotationDialog() );

		return menuItem;
	}

	private JMenuItem removeAnnotationOverlayMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Remove Annotation Overlay" );

		menuItem.addActionListener( e ->
		{
			if ( annotationOverlay != null )
			{
				annotationOverlay.close();
				annotationOverlay = null;
			}

		} );

		return menuItem;
	}

	private void overlayAnnotationDialog()
	{
		SwingUtilities.invokeLater( () ->
		{
			AnnotationOverlayDialog dialog = new AnnotationOverlayDialog( tableModel.columnNames() );

			if ( ! dialog.show() )
				return;

			if ( annotationOverlay != null )
			{
				annotationOverlay.close();
			}

			if ( tableModel.annotations().get( 0 ) instanceof AnnotatedRegion )
			{
				annotationOverlay = new AnnotatedRegionsOverlay(
						sliceViewer,
						tableModel.annotations(),
						dialog.getColumnName(),
						dialog.getFontSize() );
			}
			else
			{
				annotationOverlay = new AnnotatedSegmentsOrSpotsOverlay(
						sliceViewer,
						tableModel.annotations(),
						dialog.getColumnName() );
			}
		});
	}

	public void showContinueAnnotationDialog()
	{
		SwingUtilities.invokeLater( () ->
		{
			final String annotationColumn = UserInterfaceHelper.selectColumnNameUI(
					jTable,
					"Annotation column" );
			continueAnnotation( annotationColumn );
		});
	}

	private void selectRows( List< A > selectedRows, boolean keepCurrentSelection ) {
		if ( ! keepCurrentSelection )
			selectionModel.clearSelection();
		selectionModel.setSelected( selectedRows, true );
	}

	private void selectEqualTo()
	{
		ColumnFilteringDialog dialog = new ColumnFilteringDialog( tableModel.columnNames() );
		if ( ! dialog.show() ) return;

		final String columnName = dialog.getColumnName();
		final String value = dialog.getValue();
		final boolean keepCurrentSelection = dialog.getKeepSelected();

		ArrayList< A > selectedRows = new ArrayList<>();
		final ArrayList< A > rows = tableModel.annotations();

		final boolean isNumeric = tableModel.numericColumnNames().contains( columnName );

		double selectedNumber = 0.0;
		if ( isNumeric )
		{
			selectedNumber = Double.parseDouble( value );
			for( A row: rows )
				if ( row.getNumber( columnName ).equals( selectedNumber ) )
					selectedRows.add( row );
		}
		else
		{
			for( A row: rows )
				if ( row.getValue( columnName ).equals( value ) )
					selectedRows.add( row );
		}

		if ( !selectedRows.isEmpty() )
			selectRows( selectedRows, keepCurrentSelection );
		else
			IJ.error( value + " does not exist in column " + columnName + ", please choose another value." );
	}

	private void selectGreaterOrLessThan( final boolean greaterThan )
	{
		ColumnFilteringDialog dialog = new ColumnFilteringDialog( tableModel.numericColumnNames() );
		if ( ! dialog.show() ) return;

		final String columnName = dialog.getColumnName();
		final double numericValue = Double.parseDouble( dialog.getValue() );
		final boolean keepCurrentSelection = dialog.getKeepSelected();

		ArrayList< A > selectedRows = new ArrayList<>();
		final ArrayList< A > rows = tableModel.annotations();

		for( A row: rows )
			if ( greaterThan ?
					row.getNumber( columnName ) > numericValue :
					row.getNumber( columnName ) < numericValue )
				selectedRows.add( row );

		if ( !selectedRows.isEmpty() )
			selectRows( selectedRows, keepCurrentSelection );
		else
			if ( greaterThan )
				IJ.showMessage( "No values greater than " + numericValue + " in column " + columnName + ", please choose another value." );
			else
				IJ.showMessage("No values less than " + numericValue + " in column " + columnName + ", please choose another value.");
	}

	public void showNewAnnotationDialog()
	{
		final String columnName = showAddStringColumnDialog();
		if ( columnName == null ) return;
		continueAnnotation( columnName );
	}



	private String showAddStringColumnDialog()
	{
		final GenericDialog gd = new GenericDialog( "" );
		gd.addStringField( "Column name", "", 30 );
		gd.showDialog();
		if( gd.wasCanceled() ) return null;
		final String columnName = gd.getNextString();
		if ( tableModel.columnNames().contains( columnName ) )
		{
			IJ.error( "\"" +columnName + "\" exists already as a column name, please choose another one." );
			return null;
		}
		addStringColumn( columnName );
		return columnName;
	}

	public void continueAnnotation( String annotationColumnName )
	{
		final AnnotationUI annotationUI = new AnnotationUI(
				annotationColumnName,
				tableModel,
				selectionModel,
				jTable.getRowSorter()
		);

		// base the current coloring model
		// on the values in the annotation column
		this.coloringModel.setColoringModel( annotationUI.getColoringModel() );

		annotationUI.showDialog();
	}

	public void setVisible( boolean visible )
	{
		SwingUtilities.invokeLater(
			() ->
			{
				frame.setVisible( visible );

				if ( annotationOverlay != null )
					annotationOverlay.setVisible( visible );

				// Removed this in favor of adding back the possibility
				// to change the slice viewer visibility
				// in the UserInterfaceHelper
				// see also: https://github.com/mobie/mobie-viewer-fiji/issues/1235
//				if ( display instanceof RegionDisplay )
//				{
//					SourceAndConverterBdvDisplayService service = SourceAndConverterServices.getBdvDisplayService();
//					display.sourceAndConverters().forEach( sac -> service.setVisible( sac, visible ) );
//				}
			}
		);
	}

	public void addStringColumn( String column )
	{
		tableModel.addStringColumn( column );
	}

	private synchronized void moveToRowInView( int rowInView )
	{
		setRecentlySelectedRowInView( rowInView );
		//table.getSelectionModel().setSelectionInterval( rowInView, rowInView );
		final Rectangle visibleRect = jTable.getVisibleRect();
		final Rectangle cellRect = jTable.getCellRect( rowInView, 0, true );
		visibleRect.y = cellRect.y;
		jTable.scrollRectToVisible( visibleRect );
		jTable.repaint();
	}

	public void installSelectionModelNotification()
	{
		jTable.addMouseListener( new MouseAdapter()
		{

			@Override
			public void mouseClicked( MouseEvent e )
			{
				controlKeyPressed = e.isControlDown();
				doubleClick = e.getClickCount() == 2;
			}
		} );

		jTable.getSelectionModel().addListSelectionListener( e ->
			SwingUtilities.invokeLater( () ->
			{
				if ( selectionMode.equals( RowSelectionMode.None ) ) return;

				if ( e.getValueIsAdjusting() ) return;

				final int selectedRowInView = jTable.getSelectedRow();

				if ( selectedRowInView == -1 ) return;

				if ( selectedRowInView == recentlySelectedRowInView ) return;

				setRecentlySelectedRowInView( selectedRowInView );

				final int rowIndex = jTable.convertRowIndexToModel( recentlySelectedRowInView );

				if ( doubleClick )
				{
					// TODO don't remember what?
					int a = 1;
				}

				final A object = tableModel.annotation( rowIndex );

				selectionMode = controlKeyPressed ? RowSelectionMode.ToggleSelectionAndFocusIfSelected : RowSelectionMode.FocusOnly;

				if ( selectionMode.equals( RowSelectionMode.FocusOnly ) )
				{
					selectionModel.focus( object, this );
				}
				else
                {
                    selectionModel.toggle( object );
                    if ( selectionModel.isSelected( object ) )
                        selectionModel.focus( object, this );
                }
			})
		);
	}

	private synchronized void setRecentlySelectedRowInView( int r )
	{
		recentlySelectedRowInView = r;
	}

	private synchronized void moveToSelectedRow( A selection )
	{
		final int rowInView = jTable.convertRowIndexToView( tableModel.rowIndexOf( selection ) );

		if ( rowInView == recentlySelectedRowInView ) return;

		moveToRowInView( rowInView );
	}

	private JMenu createColoringMenu()
	{
		JMenu coloringMenu = new JMenu( "Color" );
		addColorByColumnMenuItem( coloringMenu );
		// TODO: add menu item to configure values that should be transparent
		addColorLoggingMenuItem( coloringMenu );
		return coloringMenu;
	}

	private void addColorLoggingMenuItem( JMenu coloringMenu )
	{
		final JMenuItem menuItem = new JMenuItem( "Log Current Color Map" );
		menuItem.addActionListener( e ->
				new Thread( () ->
						logCurrentValueToColorMap() ).start() );
		coloringMenu.add( menuItem );
	}

	private void logCurrentValueToColorMap()
	{
		String coloringColumnName = getColoringColumnName();

		IJ.log( " "  );
		IJ.log( "Column used for coloring: " + coloringColumnName );
		IJ.log( " "  );
		IJ.log( "Value, R, G, B"  );

		for ( int rowIndex = 0; rowIndex < tableModel.numAnnotations(); rowIndex++ )
		{
			final A annotation = tableModel.annotation( rowIndex );
			final String value = annotation.getValue( coloringColumnName ).toString();
			final ARGBType argbType = new ARGBType();
			coloringModel.convert( annotation, argbType );
			final int colorIndex = argbType.get();
			IJ.log( value + ": " + ARGBType.red( colorIndex ) + ", " + ARGBType.green( colorIndex ) + ", " + ARGBType.blue( colorIndex ) );
		}
	}

	public String getColoringColumnName()
	{
		final ColoringModel< A > coloringModel = this.coloringModel.getWrappedColoringModel();

		if ( coloringModel instanceof CategoricalAnnotationColoringModel )
		{
			return ( ( CategoricalAnnotationColoringModel ) coloringModel ).getColumnName();
		}
		else
		{
			final String msg = "Please first use the [ Color > Color by Column ] menu item to configure the coloring.";
			IJ.error( msg );
			throw new UnsupportedOperationException( msg );
		}
	}

	private void addColorByColumnMenuItem( JMenu coloringMenu )
	{
		final JMenuItem menuItem = new JMenuItem( "Color by Column..." );

		menuItem.addActionListener( e ->
				new Thread( () -> showColorByColumnDialog()
				).start() );

		coloringMenu.add( menuItem );
	}

	public void showColorByColumnDialog()
	{
		final ColoringModel< A > coloringModel =
				new ColorByColumnDialog<>( tableModel  ).show();

		if ( coloringModel != null )
			this.coloringModel.setColoringModel( coloringModel );
	}

	public Window getWindow()
	{
		return frame;
	}

	@Override
	public synchronized void selectionChanged()
	{
		if ( selectionModel.isEmpty() )
		{
			setRecentlySelectedRowInView( -1 );
			jTable.getSelectionModel().clearSelection();
		}
		repaintTable();
	}

	@Override
	public synchronized void focusEvent( A selection, Object initiator )
	{
		SwingUtilities.invokeLater( () -> moveToSelectedRow( selection ) );
	}

	@Override
	public void coloringChanged()
	{
		repaintTable();
	}

	private void configureRowColoring()
	{
		jTable.setDefaultRenderer( Double.class, new DefaultTableCellRenderer()
		{
			@Override
			public Component getTableCellRendererComponent(
					JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {

				Component c = super.getTableCellRendererComponent(
						table,
						value,
						isSelected,
						hasFocus,
						row,
						column);

				c.setBackground( getColor( row ) );

				return c;
			}
		} );

		jTable.setDefaultRenderer( String.class, new DefaultTableCellRenderer()
		{
			@Override
			public Component getTableCellRendererComponent(
					JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {

				Component c = super.getTableCellRendererComponent(
						table,
						value,
						isSelected,
						hasFocus,
						row,
						column);

				c.setBackground( getColor( row ) );

				return c;
			}

		} );

		jTable.setDefaultRenderer( Long.class, new DefaultTableCellRenderer()
		{
			@Override
			public Component getTableCellRendererComponent(
					JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column )
			{
				Component c = super.getTableCellRendererComponent(
						table,
						value,
						isSelected,
						hasFocus,
						row,
						column );

				c.setBackground( getColor( row ) );

				return c;
			}
		});

		jTable.setDefaultRenderer( Integer.class, new DefaultTableCellRenderer()
		{
			@Override
			public Component getTableCellRendererComponent(
					JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column)
			{
				Component c = super.getTableCellRendererComponent(
						table,
						value,
						isSelected,
						hasFocus,
						row,
						column);

				c.setBackground( getColor( row ) );

				return c;
			}
		} );

		jTable.setDefaultRenderer( Object.class, new DefaultTableCellRenderer()
		{
			@Override
			public Component getTableCellRendererComponent(
					JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column )
			{
				Component c = super.getTableCellRendererComponent(
						table,
						value,
						isSelected,
						hasFocus,
						row,
						column );

				c.setBackground( getColor( row ) );

				return c;
			}
		});
	}

	private Color getColor( int rowIndexInView )
	{
		final int rowIndex = jTable.convertRowIndexToModel( rowIndexInView );

		final ARGBType argbType = new ARGBType();
		final A annotation = tableModel.annotation( rowIndex );
		coloringModel.convert( annotation, argbType );

		if ( ARGBType.alpha( argbType.get() ) == 0 )
			return Color.WHITE;
		else
			return ColorHelper.getColor( argbType );
	}

	private synchronized void repaintTable()
	{
		jTable.repaint();
	}


	@Override
	public void annotationsAdded( Collection< A > annotations )
	{
		// https://github.com/mobie/mobie-viewer-fiji/issues/1146
		updateTable();
	}

	@Override
	public synchronized void columnsAdded( Collection< String > columns )
	{
		try
		{
			updateTable();
		}
		catch ( Exception e )
		{
			// TODO: errors such as
			// Exception in thread "AWT-EventQueue-0" java.lang.ArrayIndexOutOfBoundsException: 31 >= 31
			//	at java.util.Vector.elementAt(Vector.java:479)
			//	at javax.swing.table.DefaultTableColumnModel
			// For debugging:
			System.out.println("Error updating " + tableName );
			final List< String > columnNames = tableModel.columnNames();
			final int swingColumnCount = swingTableModel.getColumnCount();
			final TableColumnModel columnModel = jTable.getColumnModel();
			final int jTableColumnCount = columnModel.getColumnCount();
			IJ.wait( 100 ); // maybe this avoids the updating error?
			for ( int i = 0; i < columnNames.size(); i++ )
			{
				System.out.println( columnNames.get( i ) );
				System.out.println( swingTableModel.getColumnName( i ) );
				System.out.println( jTable.getColumnName( i ) );
			}
			throw new RuntimeException( e );
		}

	}
}
