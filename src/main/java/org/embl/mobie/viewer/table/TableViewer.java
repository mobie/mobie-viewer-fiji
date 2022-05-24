/*-
 * #%L
 * Various Java code for ImageJ
 * %%
 * Copyright (C) 2018 - 2021 EMBL
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
package org.embl.mobie.viewer.table;

import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIEHelper;
import org.embl.mobie.viewer.annotate.RegionTableRow;
import org.embl.mobie.viewer.annotate.Annotator;
import org.embl.mobie.viewer.color.SelectionColoringModel;
import de.embl.cba.tables.*;
import de.embl.cba.tables.color.*;
import de.embl.cba.tables.plot.ScatterPlotDialog;

import org.embl.mobie.viewer.select.SelectionListener;
import org.embl.mobie.viewer.select.SelectionModel;
import de.embl.cba.tables.tablerow.JTableFromTableRowsModelCreator;
import de.embl.cba.tables.tablerow.TableRow;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import de.embl.cba.tables.tablerow.TableRowListener;
import de.embl.cba.tables.TableRows;
import ij.gui.GenericDialog;
import net.imglib2.type.numeric.ARGBType;

import javax.activation.UnsupportedDataTypeException;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.embl.mobie.viewer.MoBIEHelper.*;
import static de.embl.cba.tables.color.CategoryTableRowColumnColoringModel.DARK_GREY;

public class TableViewer< T extends TableRow > implements SelectionListener< T >, ColoringListener, TableRowListener
{

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	private final MoBIE moBIE;
	private final List< T > tableRows;
	private final SelectionModel< T > selectionModel;
	private final SelectionColoringModel< T > coloringModel;
	private final String tableName;

	private JTable jTable;

	private int recentlySelectedRowInView;
	private ColumnColoringModelCreator< T > columnColoringModelCreator;
	private String mergeByColumnName; // for loading additional columns
	private Map< String, String > sourceNameToTableDir; // for loading additional columns
	private ArrayList< String > additionalTables; // tables from which additional columns are loaded
	private boolean hasColumnsFromTablesOutsideProject; // whether additional columns have been loaded from tables outside the project
	private boolean isGridTable; // Needed as merging columns to a segments table is different to a grid table
	private TableRowSelectionMode tableRowSelectionMode = TableRowSelectionMode.FocusOnly;

	// TODO: this is only for the annotator (maybe move it there)
	private Map< String, CategoryTableRowColumnColoringModel< T > > columnNameToColoringModel = new HashMap<>(  );

	private boolean controlDown;
	private JFrame frame;

	public void close()
	{
		frame.dispose();
	}

	private enum TableRowSelectionMode
	{
		None,
		FocusOnly,
		ToggleSelectionAndFocusIfSelected
	}

	public TableViewer(
			final MoBIE moBIE,
			final List< T > tableRows,
			final SelectionModel< T > selectionModel,
			final SelectionColoringModel< T > selectionColoringModel,
			String tableName,
			Map< String, String > sourceNameToTableDir,
			boolean isGridTable )
	{
		this.moBIE = moBIE;
		this.tableRows = tableRows;
		this.coloringModel = selectionColoringModel;
		this.selectionModel = selectionModel;
		this.tableName = tableName;
		this.recentlySelectedRowInView = -1;
		this.additionalTables = new ArrayList<>();
		this.sourceNameToTableDir = sourceNameToTableDir;
		this.hasColumnsFromTablesOutsideProject = false;
		this.isGridTable = isGridTable;

		// TODO: reconsider
		registerAsTableRowListener( tableRows );

		configureJTable();

		if ( selectionModel != null )
			installSelectionModelNotification();

		if ( coloringModel != null)
			configureTableRowColoring();

		createFrame();
	}

	public TableViewer< T > show()
	{
		configureJTable();

		if ( selectionModel != null )
			installSelectionModelNotification();

		if ( coloringModel != null)
			configureTableRowColoring();

		createFrame();

		return this;
	}

	public void registerAsTableRowListener( List< T > tableRows )
	{
		final int numTableRows = tableRows.size();
		for ( int rowIndex = 0; rowIndex < numTableRows; rowIndex++ )
		{
			int finalRowIndex = rowIndex;

			tableRows.get( rowIndex ).listeners().add( new TableRowListener()
			{
				@Override
				public void cellChanged( String columnName, String value )
				{
					synchronized ( jTable )
					{
						if ( ! Tables.getColumnNames( jTable ).contains( columnName ) )
						{
							Tables.addColumnToJTable( columnName, value, jTable.getModel() );
						}

						Tables.setJTableCell( finalRowIndex, columnName, value, jTable );
					}
				}
			});
		}
	}

	public List< T > getTableRows()
	{
		return tableRows;
	}


	private void configureTableRowColoring()
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

				c.setBackground( getColor(row, column) );

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

				c.setBackground( getColor(row, column) );

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

				c.setBackground( getColor( row, column ) );

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

				c.setBackground( getColor(row, column) );

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

				c.setBackground( getColor( row, column ) );

				return c;
			}
		});
	}

	private Color getColor( int rowInView, int columnInView )
	{
		final int row = jTable.convertRowIndexToModel( rowInView );

//		if ( selectionModel.isFocused( tableRows.getTableRows().get( row ) ) )
//		{
//			return Color.BLUE;
//		}

		final ARGBType argbType = new ARGBType();
		final T tableRow = tableRows.get( row );
		coloringModel.convert( tableRow, argbType );

		if ( ARGBType.alpha( argbType.get() ) == 0 )
			return Color.WHITE;
		else
			return ColorUtils.getColor( argbType );
	}

	private synchronized void repaintTable()
	{
		jTable.repaint();
	}

	private void configureJTable()
	{
		jTable = new JTableFromTableRowsModelCreator( tableRows ).createJTable();
		jTable.setPreferredScrollableViewportSize( new Dimension(500, 200) );
		jTable.setFillsViewportHeight( true );
		jTable.setAutoCreateRowSorter( true );
		jTable.setRowSelectionAllowed( true );
		jTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

		columnColoringModelCreator = new ColumnColoringModelCreator( jTable );
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

	private JMenu createAnnotateMenu()
	{
		JMenu menu = new JMenu( "Annotate" );

		menu.add( createStartNewAnnotationMenuItem() );

		menu.add( createContinueAnnotationMenuItem() );

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
		final JMenuItem menuItem = new JMenuItem( "2D Scatter Plot..." );
		menuItem.addActionListener( e ->
			{
				SwingUtilities.invokeLater( () ->
				{
					String[] columnNames = getColumnNames().stream().toArray( String[]::new );
					ScatterPlotDialog dialog = new ScatterPlotDialog( columnNames, new String[]{ columnNames[ 0 ], columnNames[ 1 ] }, new double[]{ 1.0, 1.0 }, 1.0 );

					if ( dialog.show() )
					{
//						TableRowsScatterPlot< T > scatterPlot = new TableRowsScatterPlot<>( tableRows, coloringModel, dialog.getSelectedColumns(), dialog.getScaleFactors(), dialog.getDotSizeScaleFactor() );
//						scatterPlot.show( null );
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

		return menu;
    }

    public void addAdditionalTable(String tablePath) {
		String tableName  = new File(tablePath).getName();
		additionalTables.add(tableName);
	}

	private Map< String, List< String > > openTable( String tablePath )
	{
		String resolvedPath = MoBIEHelper.resolveTablePath( tablePath );
		Logger.info( "Opening table:\n" + resolvedPath );
		return TableColumns.stringColumnsFromTableFile( resolvedPath );
	}

	private void loadColumnsFromProject()
	{
		final ArrayList< String > directories = new ArrayList<>( sourceNameToTableDir.values() );
		String tableName = selectCommonFileNameFromProject( directories, "Table" );

		ArrayList<String> tableNames = new ArrayList<>();
		if ( tableName != null )
		{
			tableNames.add( tableName );

			if ( ! isGridTable )
			{
				List< String > sources = new ArrayList<>( sourceNameToTableDir.keySet() );
				moBIE.appendSegmentsTables( sources, tableNames, (List<TableRowImageSegment>) tableRows);
			}
			else
			{
				for ( String tableDir: sourceNameToTableDir.values() )
				{
					final Map< String, List< String > > table = openTable( IOHelper.combinePath( tableDir, tableName ) );
					MoBIE.mergeRegionTables( (List< RegionTableRow >) tableRows, table );
				}
			}
			addAdditionalTable( tableName );
		}
	}

	private void loadColumnsFromFileSystem()
	{
		String path = selectFilePath( null, "Table", true );

		if ( path != null ) {
			new Thread( () -> {
				enableRowSorting( false ); // otherwise it can crash during loading.
				if ( ! isGridTable )
				{
					final String sourceName = ( String ) sourceNameToTableDir.keySet().toArray()[ 0 ];
					moBIE.appendSegmentsTables( sourceName, path, ( List< TableRowImageSegment > ) tableRows );
				}
				else
				{
					Map< String, List< String > > table = openTable( path );
					MoBIE.mergeRegionTables( ( List< RegionTableRow > ) tableRows, table );
				}
				enableRowSorting( true );
			}).start();
			hasColumnsFromTablesOutsideProject = true;
		}
	}

	private void enableRowSorting( boolean sortable )
	{
		final int columnCount = jTable.getColumnCount();
		for ( int i = 0; i < columnCount; i++ )
		{
			((DefaultRowSorter) jTable.getRowSorter()).setSortable( i, sortable );
		}
	}

	private JMenuItem createLoadColumnsMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Load Columns..." );
		menuItem.addActionListener( e ->
				new Thread( () -> {
					FileLocation fileLocation;
					if (sourceNameToTableDir.size() > 1) {
						// For multi-source tables, we only allow loading from the project
						fileLocation = FileLocation.Project;
					} else {
						fileLocation = loadFromProjectOrFileSystemDialog();
						if (fileLocation == null)
							return;
					}


					if (fileLocation == FileLocation.Project) {
						loadColumnsFromProject();
					} else {
						loadColumnsFromFileSystem();
					}
				}).start() );

		return menuItem;
	}

	public ArrayList<String> getAdditionalTables() {
		return additionalTables;
	}

	public boolean hasColumnsFromTablesOutsideProject() {
		return hasColumnsFromTablesOutsideProject;
	}

	private JMenuItem createSaveTableAsMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Save Table As..." );
		menuItem.addActionListener( e ->
				SwingUtilities.invokeLater( () ->
						TableUIs.saveTableUI( jTable ) ) );

		return menuItem;
	}

	private JMenuItem createSaveColumnsAsMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Save Columns As..." );
		menuItem.addActionListener( e ->
				SwingUtilities.invokeLater( () -> TableUIs.saveColumns( jTable ) ) );

		return menuItem;
	}

	private JMenuItem createSelectAllMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Select All" );

		menuItem.addActionListener( e ->
				SwingUtilities.invokeLater( () ->
						selectAll() ) );

		return menuItem;
	}

	private JMenuItem createSelectEqualToMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Select Equal To..." );

		menuItem.addActionListener( e ->
				SwingUtilities.invokeLater( () ->
						selectEqualTo() ) );

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

	private JMenuItem createStartNewAnnotationMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Start New Annotation..." );

		menuItem.addActionListener( e -> showNewAnnotationDialog() );

		return menuItem;
	}

	private JMenuItem createContinueAnnotationMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Continue Annotation..." );

		menuItem.addActionListener( e -> showContinueAnnotationDialog() );

		return menuItem;
	}

	public void showContinueAnnotationDialog()
	{
		SwingUtilities.invokeLater( () ->
		{
			final String annotationColumn = TableUIs.selectColumnNameUI( jTable, "Annotation column" );
			continueAnnotation( annotationColumn );
		});
	}

	private void selectAll()
	{
		selectionModel.setSelected( tableRows, true );
//		for ( T tableRow : tableRows )
//		{
//			selectionModel.setSelected( tableRow, true );
//			selectionModel.focus( tableRow );
//		}

	}

	private void selectRows( List<T> selectedTableRows, List<T> notSelectedTableRows ) {
		selectionModel.setSelected( selectedTableRows, true );
		selectionModel.setSelected( notSelectedTableRows, false );
	}

	private void selectEqualTo()
	{
		// works for categorical and numeric columns
		final GenericDialog gd = new GenericDialog( "" );
		String[] columnNames = getColumnNames().stream().toArray( String[]::new );
		gd.addChoice( "Column", columnNames, columnNames[0] );
		gd.addStringField( "value", "" );
		gd.showDialog();
		if( gd.wasCanceled() ) return;
		final String columnName = gd.getNextChoice();
		final String value = gd.getNextString();


		// Have to parse to doubles for double column (as e.g. integers like 9 are displayed as 9.0)
		double doubleValue = 0;
		boolean isDoubleColumn = jTable.getValueAt(0, jTable.getColumn( columnName ).getModelIndex() ) instanceof Double;
		if ( isDoubleColumn ) {
			try {
				doubleValue = Utils.parseDouble(value);
			} catch (NumberFormatException e) {
				Logger.error( value + " does not exist in column " + columnName + ", please choose another value." );
				return;
			}
		}

		ArrayList<T> selectedTableRows = new ArrayList<>();
		ArrayList<T> notSelectedTableRows = new ArrayList<>();
		for( T tableRow: tableRows ) {
			String tableValue = tableRow.getCell( columnName );
			boolean valuesMatch;

			if ( isDoubleColumn ) {
				double tableDouble = Utils.parseDouble( tableValue );
				valuesMatch = doubleValue == tableDouble;
			} else {
				valuesMatch = tableValue.equals( value );
			}

			if ( valuesMatch ) {
				selectedTableRows.add( tableRow );
			} else {
				notSelectedTableRows.add( tableRow );
			}
		}

		if ( selectedTableRows.size() > 0 ) {
			selectRows( selectedTableRows, notSelectedTableRows );
		} else {
			Logger.error( value + " does not exist in column " + columnName + ", please choose another value." );
		}
	}

	private void selectGreaterOrLessThan( boolean greaterThan ) {
		// only works for numeric columns
		final GenericDialog gd = new GenericDialog( "" );
		String[] columnNames = getNumericColumnNames().toArray(new String[0]);
		gd.addChoice( "Column", columnNames, columnNames[0] );
		gd.addNumericField( "value", 0 );
		gd.showDialog();
		if( gd.wasCanceled() ) return;
		final String columnName = gd.getNextChoice();
		final double value = gd.getNextNumber();

		ArrayList<T> selectedTableRows = new ArrayList<>();
		ArrayList<T> notSelectedTableRows = new ArrayList<>();
		for( T tableRow: tableRows ) {

			boolean criteriaMet;
			if ( greaterThan ) {
				criteriaMet = Utils.parseDouble(tableRow.getCell(columnName)) > value;
			} else {
				criteriaMet = Utils.parseDouble(tableRow.getCell(columnName)) < value;
			}

			if ( criteriaMet ) {
				selectedTableRows.add(tableRow);
			} else {
				notSelectedTableRows.add(tableRow);
			}
		}

		if ( selectedTableRows.size() > 0 ) {
			selectRows( selectedTableRows, notSelectedTableRows );
		} else {
			if ( greaterThan ) {
				Logger.error("No values greater than " + value + " in column " + columnName + ", please choose another value.");
			} else {
				Logger.error("No values less than " + value + " in column " + columnName + ", please choose another value.");
			}
		}
	}

	public void showNewAnnotationDialog()
	{
		final GenericDialog gd = new GenericDialog( "" );
		gd.addStringField( "Annotation column name", "", 30 );
		gd.showDialog();
		if( gd.wasCanceled() ) return;
		final String columnName = gd.getNextString();
		if ( getColumnNames().contains( columnName ) )
		{
			Logger.error( "\"" +columnName + "\" exists already as a column name, please choose another one." );
			return;
		}

		this.addColumn( columnName, "None" );

		continueAnnotation( columnName );
	}

	public void continueAnnotation( String columnName )
	{
		if ( ! columnNameToColoringModel.containsKey( columnName ) )
		{
			final CategoryTableRowColumnColoringModel< T > categoricalColoringModel = columnColoringModelCreator.createCategoricalColoringModel( columnName, false, new GlasbeyARGBLut(), DARK_GREY );
			columnNameToColoringModel.put( columnName, categoricalColoringModel );
		}

		coloringModel.setColoringModel( columnNameToColoringModel.get( columnName ) );
		final RowSorter< ? extends TableModel > rowSorter = jTable.getRowSorter();

		final Annotator annotator = new Annotator(
				columnName,
				tableRows,
				selectionModel,
				columnNameToColoringModel.get( columnName ),
				rowSorter
		);

		annotator.showDialog();
	}

	private void createFrame()
	{
		final JPanel panel = new JPanel( new GridLayout( 1, 0 ) );
		JScrollPane scrollPane = new JScrollPane(
				jTable,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		panel.add( scrollPane );

		jTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		panel.updateUI(); // TODO do we need this?
		panel.setOpaque( true );

		frame = new JFrame( tableName );
		final JMenuBar menuBar = createMenuBar();
		frame.setJMenuBar( menuBar );
		frame.setContentPane( panel );

		// Display the window.
		frame.pack();

		// Replace closing by making it invisible
		frame.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
		frame.addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent ev) {
				frame.setVisible( false );
			}
		});
	}

	public void setVisible( boolean visible )
	{
		SwingUtilities.invokeLater( () -> frame.setVisible( visible ) );
	}

	public void addColumn( String column, Object defaultValue )
	{
		if ( getColumnNames().contains( column ) )
			throw new RuntimeException( column + " exists already, please choose another name." );

		TableRows.addColumn( tableRows, column, defaultValue );
	}

	public void addColumn( String column, Object[] values )
	{
		TableRows.addColumn( tableRows, column, values );
	}

	public void addColumns( Map< String, List< String > > columns )
	{
		for ( String columnName : columns.keySet() )
		{
			try
			{
				final Object[] values = TableColumns.asTypedArray( columns.get( columnName ) );
				addColumn( columnName, values );
			} catch ( UnsupportedDataTypeException e )
			{
				Logger.error( "Could not add column " + columnName + ", because the" +
						" data type could not be determined.");
			}
		}
	}

	public Set< String > getColumnNames()
	{
		return tableRows.get( 0 ).getColumnNames();
	}

	public List<String> getNumericColumnNames()
	{
		Set<String> columnNames = getColumnNames();
		ArrayList<String> numericColumnNames = new ArrayList<>();
		for( String columnName: columnNames ) {
			if ( jTable.getValueAt(0, jTable.getColumn( columnName ).getModelIndex() ) instanceof Double ) {
				numericColumnNames.add( columnName );
			}
		}

		return numericColumnNames;
	}

	public JTable getTable()
	{
		return jTable;
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
				controlDown = e.isControlDown();
			}
		} );

		jTable.getSelectionModel().addListSelectionListener( e ->
			SwingUtilities.invokeLater( () ->
			{
				if ( tableRowSelectionMode.equals( TableRowSelectionMode.None ) ) return;

				if ( e.getValueIsAdjusting() ) return;

				final int selectedRowInView = jTable.getSelectedRow();

				if ( selectedRowInView == -1 ) return;

				if ( selectedRowInView == recentlySelectedRowInView ) return;

				setRecentlySelectedRowInView( selectedRowInView );

				final int row = jTable.convertRowIndexToModel( recentlySelectedRowInView );

				final T object = tableRows.get( row );

				tableRowSelectionMode = controlDown ? TableRowSelectionMode.ToggleSelectionAndFocusIfSelected : TableRowSelectionMode.FocusOnly;

				if ( tableRowSelectionMode.equals( TableRowSelectionMode.FocusOnly ) )
				{
					selectionModel.focus( object, this );
				}
				else if ( tableRowSelectionMode.equals( TableRowSelectionMode.ToggleSelectionAndFocusIfSelected ) )
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

	private synchronized void moveToSelectedTableRow( T selection )
	{
		final int rowInView = jTable.convertRowIndexToView( tableRows.indexOf( selection ) );

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

		if ( coloringColumnName == null )
		{
			Logger.error( "Please first use the [ Color > Color by Column ] menu item to configure the coloring." );
			return;
		}

		Logger.info( " "  );
		Logger.info( "Column used for coloring: " + coloringColumnName );
		Logger.info( " "  );
		Logger.info( "Value, R, G, B"  );

		for ( T tableRow : tableRows )
		{
			final String value = tableRow.getCell( coloringColumnName );

			final ARGBType argbType = new ARGBType();
			coloringModel.convert( tableRow, argbType );
			final int colorIndex = argbType.get();
			Logger.info( value + ": " + ARGBType.red( colorIndex ) + ", " + ARGBType.green( colorIndex ) + ", " + ARGBType.blue( colorIndex ) );
		}
	}

	public String getColoringColumnName()
	{
		final ColoringModel< T > coloringModel = this.coloringModel.getWrappedColoringModel();

		if ( coloringModel instanceof ColumnColoringModel )
		{
			return (( ColumnColoringModel ) coloringModel).getColumnName();
		}
		else
		{
			return null;
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
		final ColoringModel< T > coloringModel = columnColoringModelCreator.showDialog();

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
		SwingUtilities.invokeLater( () -> repaintTable() );
	}

	@Override
	public synchronized void focusEvent( T selection, Object initiator )
	{
		SwingUtilities.invokeLater( () -> moveToSelectedTableRow( selection ) );
	}

	@Override
	public void coloringChanged()
	{
		SwingUtilities.invokeLater( () -> repaintTable() );
	}

	@Override
	public void cellChanged( String columnName, String value )
	{
		// TODO:
	}

}
