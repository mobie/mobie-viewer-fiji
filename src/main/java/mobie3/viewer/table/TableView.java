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
package mobie3.viewer.table;

import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.tables.Logger;
import de.embl.cba.tables.TableUIs;
import de.embl.cba.tables.Utils;
import de.embl.cba.tables.color.ColorUtils;
import de.embl.cba.tables.color.ColoringListener;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.plot.ScatterPlotDialog;
import de.embl.cba.tables.tablerow.TableRowListener;
import ij.gui.GenericDialog;
import mobie3.viewer.MoBIE;
import mobie3.viewer.annotate.Annotator;
import mobie3.viewer.color.CategoricalColoringModel;
import mobie3.viewer.color.ColumnColoringModelCreator;
import mobie3.viewer.color.SelectionColoringModel;
import mobie3.viewer.display.AnnotationDisplay;
import mobie3.viewer.select.SelectionListener;
import mobie3.viewer.select.SelectionModel;
import net.imglib2.type.numeric.ARGBType;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static de.embl.cba.tables.color.CategoryTableRowColumnColoringModel.DARK_GREY;
import static org.embl.mobie.viewer.MoBIEHelper.FileLocation;
import static org.embl.mobie.viewer.MoBIEHelper.loadFromProjectOrFileSystemDialog;

public class TableView< A extends Annotation > implements SelectionListener< A >, ColoringListener, TableRowListener
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	private final MoBIE moBIE;
	private final AnnotationTableModel< A > tableModel;
	private final SelectionModel< A > selectionModel;
	private final SelectionColoringModel< A > coloringModel;
	private final String tableName;
	private final AnnotationDisplay< A > display;
	private JTable jTable;

	private int recentlySelectedRowInView;
	private ColumnColoringModelCreator< A > columnColoringModelCreator;
	private ArrayList< String > additionalTables = new ArrayList<>();; // tables from which additional columns are loaded
	private boolean hasColumnsFromTablesOutsideProject; // whether additional columns have been loaded from tables outside the project
	private TableRowSelectionMode tableRowSelectionMode = TableRowSelectionMode.FocusOnly;

	// TODO: this is only for the annotator (maybe move it there)
	private Map< String, CategoricalColoringModel< A > > columnNameToColoringModel = new HashMap<>();

	private boolean controlDown;
	private JFrame frame;

	private enum TableRowSelectionMode
	{
		None,
		FocusOnly,
		ToggleSelectionAndFocusIfSelected
	}

	public TableView( MoBIE moBIE, AnnotationDisplay< A > display )
	{
		this.moBIE = moBIE;
		this.display = display;
		this.tableModel = display.tableModel;
		this.coloringModel = display.coloringModel;
		this.selectionModel = display.selectionModel;
		this.tableName = display.getName();
		this.recentlySelectedRowInView = -1;

		// TODO: probably we can get rid of this entirely,
		//  because changing tableRows now works through the
		//  TableRowsTableModel which has a direct relation to the JTable
		//  However I am not sure right now who else
		//  (but the TableViewer) may change TableRows..
		//registerAsTableRowListener( tableRows );

		configureJTable();

		if ( selectionModel != null )
			installSelectionModelNotification();

		if ( coloringModel != null)
			configureTableRowColoring();
	}

	public void close()
	{
		frame.dispose();
	}

	public void show()
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

		// Display the window
		frame.pack();

		// Replace closing by making it invisible
		frame.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
		frame.addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent ev) {
				frame.setVisible( false );
			}
		});
	}

//	public void registerAsTableRowListener( List< T > tableRows )
//	{
//		final int numTableRows = tableRows.size();
//		for ( int rowIndex = 0; rowIndex < numTableRows; rowIndex++ )
//		{
//			int finalRowIndex = rowIndex;
//
//			tableRows.get( rowIndex ).listeners().add( new TableRowListener()
//			{
//				@Override
//				public void cellChanged( String columnName, String value )
//				{
//					synchronized ( jTable )
//					{
//						if ( ! tableRowsTableModel.getColumnNames().contains( columnName ) )
//						{
//							// TODO: how to add a column?
//							//   have to check implementations of TableRow
//							tableRowsTableModel.setValueAt( value, finalRowIndex, tableRowsTableModel.getColumnNames().indexOf( columnName ) );
//							//Tables.addColumnToJTable( columnName, value, jTable.getModel() );
//						}
//
//						// TODO: 2021: Change this! 2022: Why?
//						tableRowsTableModel.setValueAt( value, finalRowIndex, tableRowsTableModel.getColumnNames().indexOf( columnName ) );
////						Tables.setJTableCell( finalRowIndex, columnName, value, jTable );
//					}
//				}
//			});
//		}
//	}
//
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
		final A tableRow = tableModel.row( row );
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
		final SwingTableModel swingTableModel = new SwingTableModel( tableModel );
		jTable = new JTable( swingTableModel );
		jTable.setPreferredScrollableViewportSize( new Dimension(500, 200) );
		jTable.setFillsViewportHeight( true );
		jTable.setAutoCreateRowSorter( true );
		jTable.setRowSelectionAllowed( true );
		jTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

		// TODO: do this with TableRowsModel instead ?!
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
					String[] columnNames = tableModel.columnNames().stream().toArray( String[]::new );
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

	public void enableRowSorting( boolean sortable )
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
				FileLocation fileLocation = loadFromProjectOrFileSystemDialog();
				// TODO
				throw new UnsupportedOperationException("Column loading not yet implemented");
//				if ( fileLocation.equals( FileLocation.Project ) )
//				{
//					tableModel.columnPaths();
//					tableModel.loadColumns(  );
//					moBIE.mergeColumnsFromProject( display );
//				}
//				else if ( fileLocation.equals( FileLocation.FileSystem ))
//				{
//					moBIE.mergeColumnsFromFileSystem( display );
//				}
			}).start()
		);

		return menuItem;
	}

	public ArrayList<String> getAdditionalTables()
	{
		return additionalTables;
	}

	public boolean hasColumnsFromTablesOutsideProject()
	{
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
						selectionModel.setSelected( tableModel.rows(), true ) ) );

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

	private void selectRows( List< A > selectedTableRows, List< A > notSelectedTableRows ) {
		selectionModel.setSelected( selectedTableRows, true );
		selectionModel.setSelected( notSelectedTableRows, false );
	}

	private void selectEqualTo()
	{
		// works for categorical and numeric columns
		final GenericDialog gd = new GenericDialog( "" );
		String[] columnNames = tableModel.columnNames().stream().toArray( String[]::new );
		gd.addChoice( "Column", columnNames, columnNames[0] );
		gd.addStringField( "value", "" );
		gd.showDialog();
		if( gd.wasCanceled() ) return;
		final String columnName = gd.getNextChoice();
		final String value = gd.getNextString();


		// Have to parse to doubles for double column (as e.g. integers like 9 are displayed as 9.0)
		// TODO! we know now the classes of the columns!
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

		ArrayList< A > selectedTableRows = new ArrayList<>();
		ArrayList< A > notSelectedTableRows = new ArrayList<>();
		final Set< A > rows = tableModel.rows();
		for( A row: rows ) {
			boolean valuesMatch;

			if ( tableModel.columnClass( columnName ) == Double.class ) {
				double tableDouble = ( Double ) row.getValue( columnName );
				valuesMatch = doubleValue == tableDouble;
			} else {
				valuesMatch = row.getValue( columnName ).equals( value );
			}

			if ( valuesMatch ) {
				selectedTableRows.add( row );
			} else {
				notSelectedTableRows.add( row );
			}
		}

		if ( selectedTableRows.size() > 0 ) {
			selectRows( selectedTableRows, notSelectedTableRows );
		} else {
			Logger.error( value + " does not exist in column " + columnName + ", please choose another value." );
		}
	}

	// this could be delegated to TableSaw ?
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

		ArrayList< A > selectedTableRows = new ArrayList<>();
		ArrayList< A > notSelectedTableRows = new ArrayList<>();
		final Set< A > rows = tableModel.rows();
		for( A row: rows ) {

			boolean criteriaMet;
			if ( greaterThan ) {
				criteriaMet = (Double) row.getValue(columnName) > value;
			} else {
				criteriaMet = (Double) row.getValue(columnName) < value;
			}

			if ( criteriaMet ) {
				selectedTableRows.add(row);
			} else {
				notSelectedTableRows.add(row);
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
		if ( tableModel.columnNames().contains( columnName ) )
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
			final CategoricalColoringModel< A > categoricalColoringModel = columnColoringModelCreator.createCategoricalColoringModel( columnName, false, new GlasbeyARGBLut(), DARK_GREY );
			columnNameToColoringModel.put( columnName, categoricalColoringModel );
		}

		coloringModel.setColoringModel( columnNameToColoringModel.get( columnName ) );
		final RowSorter< ? extends TableModel > rowSorter = jTable.getRowSorter();

		final Annotator annotator = new Annotator(
				columnName,
				tableModel,
				selectionModel,
				columnNameToColoringModel.get( columnName ),
				rowSorter
		);

		annotator.showDialog();
	}

	public void setVisible( boolean visible )
	{
		SwingUtilities.invokeLater( () -> frame.setVisible( visible ) );
	}

	public void addColumn( String column, Object defaultValue )
	{
		if ( tableModel.getColumnNames().contains( column ) )
			throw new RuntimeException( column + " exists already, please choose another name." );
		tableModel.addColumn( column, defaultValue.toString() );
	}


	public List< String > getNumericColumnNames()
	{
		final List< String > columnNames = tableModel.getColumnNames();
		ArrayList< String > numericColumnNames = new ArrayList<>();
		for ( String columnName : columnNames )
		{
			if ( tableModel.isNumeric( columnName ) )
				numericColumnNames.add( columnName );
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

				final A object = tableModel.get( row );

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

	private synchronized void moveToSelectedTableRow( A selection )
	{
		final int rowInView = jTable.convertRowIndexToView( tableModel.indexOf( selection ) );

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

		for ( A tableRow : tableModel )
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
		final ColoringModel< A > coloringModel = this.coloringModel.getWrappedColoringModel();

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
		final ColoringModel< A > coloringModel = columnColoringModelCreator.showDialog();

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
	public synchronized void focusEvent( A selection, Object initiator )
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
