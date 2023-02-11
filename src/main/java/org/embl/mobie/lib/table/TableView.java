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
package org.embl.mobie.lib.table;

import de.embl.cba.tables.Logger;
import de.embl.cba.tables.TableUIs;
import ij.IJ;
import ij.gui.GenericDialog;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.annotation.AnnotationUI;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.color.CategoricalAnnotationColoringModel;
import org.embl.mobie.lib.color.ColorHelper;
import org.embl.mobie.lib.color.ColoringListener;
import org.embl.mobie.lib.color.ColoringModel;
import org.embl.mobie.lib.color.MobieColoringModel;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.plot.ScatterPlotSettings;
import org.embl.mobie.lib.serialize.display.AbstractAnnotationDisplay;
import org.embl.mobie.lib.plot.ScatterPlotDialog;
import org.embl.mobie.lib.plot.ScatterPlotView;
import org.embl.mobie.lib.select.SelectionListener;
import org.embl.mobie.lib.select.SelectionModel;
import org.embl.mobie.lib.ui.ColumnColoringModelDialog;
import net.imglib2.type.numeric.ARGBType;
import org.embl.mobie.lib.ui.UserInterfaceHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.embl.mobie.lib.MoBIEHelper.FileLocation;
import static org.embl.mobie.lib.ui.UserInterfaceHelper.loadFromProjectOrFileSystemDialog;

public class TableView< A extends Annotation > implements SelectionListener< A >, ColoringListener, AnnotationListener< A >
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	private final AnnotationTableModel< A > tableModel;
	private final SelectionModel< A > selectionModel;
	private final MobieColoringModel< A > coloringModel;
	private final String tableName;
	private JTable jTable;
	private int recentlySelectedRowInView;
	private RowSelectionMode selectionMode = RowSelectionMode.FocusOnly;
	private JFrame frame;
	private boolean controlKeyPressed;
	private SwingTableModel swingTableModel;

	private enum RowSelectionMode
	{
		None,
		FocusOnly,
		ToggleSelectionAndFocusIfSelected
	}

	public TableView( AbstractAnnotationDisplay< A > display )
	{
		this.tableModel = display.getAnnData().getTable();
		this.coloringModel = display.coloringModel;
		this.selectionModel = display.selectionModel;
		this.tableName = display.getName();
		this.recentlySelectedRowInView = -1;

		tableModel.addAnnotationListener( this );
		configureJTable();
		installSelectionModelNotification();
		configureRowColoring();
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
		panel.updateUI();
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

	private void configureJTable()
	{
		swingTableModel = new SwingTableModel( tableModel );
		jTable = new JTable( swingTableModel );
		jTable.updateUI();
		jTable.setPreferredScrollableViewportSize( new Dimension(500, 200) );
		jTable.setFillsViewportHeight( true );
		jTable.setAutoCreateRowSorter( true );
		jTable.setRowSelectionAllowed( true );
		jTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
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
		menu.add( startNewAnnotationMenuItem() );
		menu.add( continueAnnotationMenuItem() );
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
					String[] columnNames = tableModel.columnNames().stream().toArray( String[]::new );
					final ScatterPlotSettings settings = new ScatterPlotSettings( new String[]{ columnNames[ 0 ], columnNames[ 1 ] } );
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

				if ( fileLocation.equals( FileLocation.Project ) )
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
				else if ( fileLocation.equals( FileLocation.FileSystem ) )
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
				updateJTable();

			}).start()
		);
		return menuItem;
	}

	private void updateJTable()
	{
		if ( jTable == null ) return;
		jTable.tableChanged( null );
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
						selectionModel.setSelected( tableModel.annotations(), true ) ) );
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

	public void showContinueAnnotationDialog()
	{
		SwingUtilities.invokeLater( () ->
		{
			final String annotationColumn = TableUIs.selectColumnNameUI( jTable, "Annotation column" );
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
		// works for categorical and numeric columns
		final GenericDialog gd = new GenericDialog( "" );
		String[] columnNames = tableModel.columnNames().stream().toArray( String[]::new );
		gd.addChoice( "Column", columnNames, columnNames[0] );
		gd.addStringField( "value", "" );
		gd.addCheckbox( "Keep current selection", true );
		gd.showDialog();
		if( gd.wasCanceled() ) return;
		final String columnName = gd.getNextChoice();
		final String selectedValue = gd.getNextString();
		final boolean keepCurrentSelection = gd.getNextBoolean();

		ArrayList< A > selectedRows = new ArrayList<>();
		final ArrayList< A > rows = tableModel.annotations();

		final boolean numeric = tableModel.numericColumnNames().contains( columnName ) ? true : false;

		double selectedNumber = 0.0;
		if ( numeric )
		{
			selectedNumber = Double.parseDouble( selectedValue );
			for( A row: rows )
				if ( row.getNumber( columnName ).equals( selectedNumber ) )
					selectedRows.add( row );
		}
		else
		{
			for( A row: rows )
				if ( row.getValue( columnName ).equals( selectedValue ) )
					selectedRows.add( row );
		}

		if ( selectedRows.size() > 0 )
			selectRows( selectedRows, keepCurrentSelection );
		else
			Logger.error( selectedValue + " does not exist in column " + columnName + ", please choose another value." );
	}

	// TODO Code duplication with the method above
	private void selectGreaterOrLessThan( final boolean greaterThan )
	{
		// only works for numeric columns
		final GenericDialog gd = new GenericDialog( "" );
		String[] columnNames = tableModel.numericColumnNames().toArray(new String[0]);
		gd.addChoice( "Column", columnNames, columnNames[0] );
		gd.addNumericField( "value", 0 );
		gd.addCheckbox( "Keep current selection", true );
		gd.showDialog();
		if( gd.wasCanceled() ) return;
		final String columnName = gd.getNextChoice();
		final double value = gd.getNextNumber();
		final boolean keepCurrentSelection = gd.getNextBoolean();

		ArrayList< A > selectedRows = new ArrayList<>();
		final ArrayList< A > rows = tableModel.annotations();

		for( A row: rows )
			if ( greaterThan ?
					row.getNumber( columnName ) > value :
					row.getNumber( columnName ) < value )
				selectedRows.add( row );

		if ( selectedRows.size() > 0 )
			selectRows( selectedRows, keepCurrentSelection );
		else
			if ( greaterThan )
				IJ.showMessage( "No values greater than " + value + " in column " + columnName + ", please choose another value." );
			else
				IJ.showMessage("No values less than " + value + " in column " + columnName + ", please choose another value.");
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
		addStringColumn( columnName );
		continueAnnotation( columnName );
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
		SwingUtilities.invokeLater( () -> frame.setVisible( visible ) );
	}

	public void addStringColumn( String column )
	{
		// TODO: update JTable?!
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

				final A object = tableModel.annotation( rowIndex );

				selectionMode = controlKeyPressed ? RowSelectionMode.ToggleSelectionAndFocusIfSelected : RowSelectionMode.FocusOnly;

				if ( selectionMode.equals( RowSelectionMode.FocusOnly ) )
				{
					selectionModel.focus( object, this );
				}
				else if ( selectionMode.equals( RowSelectionMode.ToggleSelectionAndFocusIfSelected ) )
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

		Logger.info( " "  );
		Logger.info( "Column used for coloring: " + coloringColumnName );
		Logger.info( " "  );
		Logger.info( "Value, R, G, B"  );

		for ( int rowIndex = 0; rowIndex < tableModel.numAnnotations(); rowIndex++ )
		{
			final A annotation = tableModel.annotation( rowIndex );
			final String value = annotation.getValue( coloringColumnName ).toString();
			final ARGBType argbType = new ARGBType();
			coloringModel.convert( annotation, argbType );
			final int colorIndex = argbType.get();
			Logger.info( value + ": " + ARGBType.red( colorIndex ) + ", " + ARGBType.green( colorIndex ) + ", " + ARGBType.blue( colorIndex ) );
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
			Logger.error( msg );
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
		final ColoringModel< A > coloringModel = new ColumnColoringModelDialog<>( tableModel  ).showDialog();

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
		SwingUtilities.invokeLater( () -> jTable.repaint() );
	}

	@Override
	public void annotationsAdded( Collection< A > annotations )
	{
		updateJTable();
	}

	@Override
	public void columnAdded( String columnName )
	{
		updateJTable();
		final List< String > columnNames = tableModel.columnNames();
		for ( int i = 0; i < columnNames.size(); i++ )
		{
			System.out.println( columnNames.get( i ) );
			System.out.println( swingTableModel.getColumnName( i ) );
			System.out.println( jTable.getColumnName( i ) );
		}
	}
}
