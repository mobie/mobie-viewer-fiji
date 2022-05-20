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
package org.embl.mobie.viewer.annotate;

import de.embl.cba.tables.Logger;
import de.embl.cba.tables.SwingUtils;
import de.embl.cba.tables.color.CategoryTableRowColumnColoringModel;
import de.embl.cba.tables.color.ColorUtils;
import org.embl.mobie.viewer.select.SelectionListener;
import org.embl.mobie.viewer.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRow;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij.IJ;
import net.imglib2.type.numeric.ARGBType;
import org.embl.mobie.viewer.ui.MoBIELookAndFeel;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Annotator< T extends TableRow > extends JFrame implements SelectionListener< T >
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public static final String LAST = "You are already at the last object in table.";
	public static final String NO_MORE_SEGMENTS = "No more segments.";
	private final String annotationColumnName;
	private final List< T > tableRows;
	private final SelectionModel< T > selectionModel;
	private final CategoryTableRowColumnColoringModel< T > coloringModel;
	private final RowSorter< ? extends TableModel > rowSorter;
	private final JPanel panel;
	private boolean skipNone;
	private boolean isKeepSelectedMode = false;
	private JTextField goToRowIndexTextField;
	private HashMap< String, T > annotationToTableRow;
	private JPanel annotationButtonsContainer;
	private JScrollPane annotationButtonsScrollPane;
	private T currentlySelectedRow;
	private Set< String > annotationNames;
	private String objectName = "entity";

	public Annotator( String columnName, List< T > tableRows, SelectionModel< T > selectionModel, CategoryTableRowColumnColoringModel< T > coloringModel, RowSorter< ? extends TableModel > rowSorter )
	{
		super("");
		this.annotationColumnName = columnName;
		this.annotationNames = new HashSet<>();
		this.tableRows = tableRows;
		this.selectionModel = selectionModel;
		this.coloringModel = coloringModel;
		this.rowSorter = rowSorter;
		this.currentlySelectedRow = tableRows.get( rowSorter.convertRowIndexToModel( 0 ) );
		this.coloringModel.fixedColorMode( true );
		setNames( tableRows );
		selectionModel.listeners().add( this );

		this.panel = new JPanel();
	}

	private void setNames( List< T > tableRows )
	{
		if ( tableRows.get( 0 ) instanceof TableRowImageSegment )
		{
			objectName = "segment";
		}
		else if ( tableRows.get( 0 ) instanceof AnnotatedMaskTableRow )
		{
			objectName = "region";
		}
	}

	public void showDialog()
	{
		MoBIELookAndFeel.MoBIELafOn();
		createDialog();
		showFrame();
		MoBIELookAndFeel.MoBIELafOff();
	}

	private void createDialog()
	{
		this.setContentPane( panel );
		panel.setOpaque( true ); //content panes must be opaque
		panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );
		addCreateCategoryButton();
		panel.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		addAnnotationButtons();
		panel.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		addSelectPreviousAndNextPanel();
		addSelectIDPanel();
		addSkipNonePanel();
		addKeepSelectedPanel();
		// The annotation button panel has to be added at the end
		// to make the packing work correctly.
		// Otherwise, continuing an annotation with many categories will be
		// packed to a size too large for the screen.
		addAnnotationButtonPanels();
	}

	private void showFrame()
	{
		this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		this.setLocation( MouseInfo.getPointerInfo().getLocation().x, MouseInfo.getPointerInfo().getLocation().y );
		this.setVisible( true );
	}

	private void addCreateCategoryButton()
	{
		final JPanel panel = SwingUtils.horizontalLayoutPanel();
		final JButton button = new JButton( "Create new category" );
		final JTextField textField = new JTextField( "Class A" );
		panel.add( button );
		panel.add( textField );
		button.addActionListener( e -> {
			String newClassName = textField.getText();
			if ( getAnnotations().containsKey( newClassName ) || annotationNames.contains( newClassName )  )
			{
				Logger.error( "Category " + newClassName + " exists already." );
				return;
			}
			addAnnotationButtonPanel( newClassName, null );
		} );
		this.panel.add( panel );
	}

	private void addAnnotationButtons()
	{
		JPanel annotationButtonsPanel = new JPanel( );
		annotationButtonsPanel.setLayout( new BoxLayout(annotationButtonsPanel, BoxLayout.Y_AXIS ) );
		annotationButtonsPanel.setBorder( BorderFactory.createEmptyBorder(0,10,10,10) );
		panel.add( annotationButtonsPanel );

		final JPanel panel = SwingUtils.horizontalLayoutPanel();
		panel.add( new JLabel( "Annotate selected " + objectName + "(s) as:" ) );
		panel.add( new JLabel( "      " ) );
		annotationButtonsPanel.add( panel );

		annotationButtonsScrollPane = new JScrollPane( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		annotationButtonsScrollPane.setBorder( BorderFactory.createEmptyBorder() );
		annotationButtonsPanel.add( annotationButtonsScrollPane );

		annotationButtonsContainer = new JPanel();
		annotationButtonsContainer.setLayout( new BoxLayout( annotationButtonsContainer, BoxLayout.PAGE_AXIS ));
		annotationButtonsContainer.setBorder( BorderFactory.createEmptyBorder() );
		annotationButtonsScrollPane.setViewportView( annotationButtonsContainer );
	}

	private void addAnnotationButtonPanels() {
		final HashMap< String, T > annotations = getAnnotations();
		for ( String annotation : annotations.keySet() )
			addAnnotationButtonPanel( annotation, annotations.get( annotation ) );
	}

	private void addAnnotationButtonPanel( String annotationName, T tableRow )
	{
		MoBIELookAndFeel.MoBIELafOn();

		annotationNames.add( annotationName );

		final JPanel panel = SwingUtils.horizontalLayoutPanel();
		final JButton annotateButton = new JButton( String.format("%1$15s", annotationName) );
		annotateButton.setFont( new Font("monospaced", Font.PLAIN, 12) );
		annotateButton.setOpaque( true );
		setButtonColor( annotateButton, tableRow );
		annotateButton.setAlignmentX( Component.CENTER_ALIGNMENT );

		final ARGBType argbType = new ARGBType();
		coloringModel.convert( annotationName, argbType );
		annotateButton.setBackground( ColorUtils.getColor( argbType ) );

		annotateButton.addActionListener( e ->
		{
			if ( selectionModel.isEmpty() ) return; // nothing selected to be annotated

			final Set< T > selected = selectionModel.getSelected();

			for ( T row : selected )
			{
				row.setCell( annotationColumnName, annotationName );
			}

			if( isKeepSelectedMode )
			{
				selectionModel.clearSelection(); // Hack to notify all listeners that the coloring might have changed.
				// select again such that the user could still change its mind
				selectionModel.setSelected( selected, true );
			}
			else
			{
				selectionModel.clearSelection();
			}
		} );

		final JButton changeColor = new JButton( "C" );
		changeColor.addActionListener( e -> {
			Color color = JColorChooser.showDialog( this.panel, "", null );
			if ( color == null ) return;
			annotateButton.setBackground( color );
			coloringModel.putInputToFixedColor( annotationName, ColorUtils.getARGBType( color ) );
		} );

		panel.add( annotateButton );
		panel.add( changeColor );
		annotationButtonsContainer.add( panel );
		refreshDialog();
		MoBIELookAndFeel.MoBIELafOff();
	}

	private void addSelectPreviousAndNextPanel( )
	{
		final JPanel panel = SwingUtils.horizontalLayoutPanel();
		final JButton clear = createClearSelectionButton();
		final JButton previous = createSelectPreviousButton();
		final JButton next = createSelectNextButton();
		panel.add( clear );
		panel.add( previous );
		panel.add( next );
		this.panel.add( panel );
	}

	private void addSelectIDPanel( )
	{
		final JPanel panel = SwingUtils.horizontalLayoutPanel();
		final JButton button = createSelectButton();
		goToRowIndexTextField = new JTextField( "" );
		goToRowIndexTextField.setText( "1" );
		panel.add( button );
		panel.add( goToRowIndexTextField );
		this.panel.add( panel );
	}

	private JButton createSelectNextButton()
	{
		final JButton next = new JButton( "Select next" );
		//next.setFont( new Font("monospaced", Font.PLAIN, 12) );
		next.setAlignmentX( Component.CENTER_ALIGNMENT );

		next.addActionListener( e ->
		{
			// rowIndex in sorted "units"
			int rowIndex = rowSorter.convertRowIndexToView( tableRows.indexOf( currentlySelectedRow ) );
			if ( rowIndex < tableRows.size() - 1 )
			{
				T row = null;
				if ( skipNone )
				{
					while ( rowIndex < tableRows.size() - 1 )
					{
						row = tableRows.get( rowSorter.convertRowIndexToModel( ++rowIndex ) );
						if ( isNoneOrNan( row ) )
						{
							row = null;
							continue;
						} else
						{
							break;
						}
					}

					if ( row == null )
					{
						IJ.showMessage( NO_MORE_SEGMENTS );
						return; // All following rows are None or NaN
					}

					selectRow( row );
				} else
				{
					row = tableRows.get( rowSorter.convertRowIndexToModel( ++rowIndex ) );
					selectRow( row );
				}
			} else
			{
				IJ.showMessage( NO_MORE_SEGMENTS );
			}
		} );
		return next;
	}

	private JButton createClearSelectionButton()
	{
		final JButton clear = new JButton( "Clear selection" );
		//previous.setFont( new Font("monospaced", Font.PLAIN, 12) );
		clear.setAlignmentX( Component.CENTER_ALIGNMENT );

		clear.addActionListener( e ->
		{
			selectionModel.clearSelection();
		} );

		return clear;
	}


	private JButton createSelectPreviousButton()
	{
		final JButton previous = new JButton( "Select previous" );
		//previous.setFont( new Font("monospaced", Font.PLAIN, 12) );
		previous.setAlignmentX( Component.CENTER_ALIGNMENT );

		previous.addActionListener( e ->
		{
			// row index in sorted "units"
			int rowIndex = rowSorter.convertRowIndexToView( tableRows.indexOf( currentlySelectedRow ) );
			if ( rowIndex > 0 )
			{
				T row = null;
				if ( skipNone )
				{
					while ( rowIndex > 0 )
					{
						row = tableRows.get( rowSorter.convertRowIndexToModel( --rowIndex ) );
						if ( isNoneOrNan( row ) )
						{
							row = null;
							continue;
						}
						else
							break;
					}

					if ( row == null )
					{
						IJ.showMessage( NO_MORE_SEGMENTS );
						return; // None of the previous rows is not None
					}

					selectRow( row );
				}
				else
				{
					row = tableRows.get( rowSorter.convertRowIndexToModel( --rowIndex ) );
					selectRow( row );
				}
			}
			else
			{
				IJ.showMessage( NO_MORE_SEGMENTS );
			}
		} );

		return previous;
	}

	private JButton createSelectButton()
	{
		final JButton button = new JButton( "Select " + objectName + " ID" );
		button.setAlignmentX( Component.CENTER_ALIGNMENT );

		button.addActionListener( e ->
		{
			T selectedRow = getSelectedRow();
			if ( selectedRow != null ) selectRow( selectedRow );
		} );
		return button;
	}

	private T getSelectedRow()
	{
		if ( tableRows.get( 0 ) instanceof TableRowImageSegment )
		{
			final double selectedLabelId = Double.parseDouble( goToRowIndexTextField.getText() );
			for ( T tableRow : tableRows )
			{
				final double labelId = ( ( TableRowImageSegment ) tableRow ).labelId();
				if ( labelId == selectedLabelId )
				{
					return tableRow;
				}
			}
			throw new UnsupportedOperationException( "Could not find " + objectName + " with ID " + selectedLabelId );
		}
		else if ( tableRows.get( 0 ) instanceof AnnotatedMaskTableRow )
		{
			final String annotationID = goToRowIndexTextField.getText();
			for ( T tableRow : tableRows )
			{
				final String name = ( ( AnnotatedMaskTableRow ) tableRow ).name();
				if ( name.equals( annotationID ) )
				{
					return tableRow;
				}
			}
			throw new UnsupportedOperationException( "Could not find " + objectName + " with ID " + annotationID );
		}
		else
		{
			final int rowIndex = Integer.parseInt( goToRowIndexTextField.getText() );
			return tableRows.get( rowSorter.convertRowIndexToModel( rowIndex ) );
		}
	}

	private boolean isNoneOrNan( T row )
	{
		return row.getCell( annotationColumnName ).toLowerCase().equals( "none" )
				|| row.getCell( annotationColumnName ).toLowerCase().equals( "nan" );
	}

	private void selectRow( T row )
	{
		currentlySelectedRow = row;
		selectionModel.clearSelection();
		selectionModel.setSelected( row, true );
		selectionModel.focus( row, this );
	}

	private void addSkipNonePanel( )
	{
		final JPanel panel = SwingUtils.horizontalLayoutPanel();

		final JCheckBox checkBox = new JCheckBox( "Skip \"None\" & \"NaN\"" );
		checkBox.setSelected( false );
		skipNone = checkBox.isSelected();

		checkBox.addActionListener( e -> {
			skipNone = checkBox.isSelected();
		}  );

		panel.add( checkBox );
		this.panel.add( panel );
	}

	private void addKeepSelectedPanel( )
	{
		final JPanel panel = SwingUtils.horizontalLayoutPanel();

		final JCheckBox checkBox = new JCheckBox( "Keep "+ objectName +"(s) selected after assignment" );
		checkBox.setSelected( false );
		isKeepSelectedMode = checkBox.isSelected();

		checkBox.addActionListener( e -> {
			isKeepSelectedMode = checkBox.isSelected();
		}  );

		panel.add( checkBox );
		this.panel.add( panel );
	}

	private void setButtonColor( JButton button, T tableRow )
	{
		if ( tableRow != null )
		{
			final ARGBType argbType = new ARGBType();
			coloringModel.convert( tableRow, argbType );
			button.setBackground( new Color( argbType.get() ) );
		}
	}

	private HashMap< String, T > getAnnotations()
	{
		if ( annotationToTableRow == null )
		{
			annotationToTableRow = new HashMap<>();

			for ( int row = 0; row < tableRows.size(); row++ )
			{
				final T tableRow = tableRows.get( row );
				annotationToTableRow.put( tableRow.getCell( annotationColumnName ), tableRow );
			}
		}

		return annotationToTableRow;
	}

	private void refreshDialog()
	{
		panel.revalidate();
		panel.repaint();

		// scroll to bottom, so any new panels are visible
		annotationButtonsScrollPane.validate();
		JScrollBar vertical = annotationButtonsScrollPane.getVerticalScrollBar();
		vertical.setValue( vertical.getMaximum() );

		// scroll pane resizes up to five annotations, then requires user resizing
		if ( annotationButtonsContainer.getComponentCount() < 6 ) {
			this.pack();
		}
	}

	@Override
	public void selectionChanged()
	{

	}

	@Override
	public void focusEvent( T selection, Object initiator )
	{
		currentlySelectedRow = selection;
	}
}
