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
package org.embl.mobie.lib.annotation;

import ij.IJ;
import net.imglib2.type.numeric.ARGBType;
import org.embl.mobie.lib.color.CategoricalAnnotationColoringModel;
import org.embl.mobie.lib.color.ColorHelper;
import org.embl.mobie.lib.color.ColoringModels;
import org.embl.mobie.lib.select.SelectionListener;
import org.embl.mobie.lib.select.SelectionModel;
import org.embl.mobie.lib.table.AnnotationTableModel;
import org.embl.mobie.lib.table.DefaultValues;
import org.embl.mobie.ui.MoBIELaf;
import org.embl.mobie.ui.SwingHelper;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static org.embl.mobie.lib.color.lut.LUTs.DARK_GREY;
import static org.embl.mobie.lib.color.lut.LUTs.GLASBEY;

public class AnnotationUI< A extends Annotation > extends JFrame implements SelectionListener< A >
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public static final String LAST = "You are already at the last object in table.";
	public static final String NO_MORE_SEGMENTS = "No more segments.";
	private final String annotationColumnName;
	private final AnnotationTableModel< A > tableModel;
	private final SelectionModel< A > selectionModel;
	private final CategoricalAnnotationColoringModel< A > coloringModel;
	private final RowSorter< ? extends TableModel > rowSorter;
	private final JPanel panel;
	private static Map< String, CategoricalAnnotationColoringModel > columnNameToColoringModel = new HashMap<>();
	private boolean skipNone;
	private boolean isKeepSelectedMode = false;
	private JTextField selectAnnotationTextField;
	private JPanel annotationButtonsContainer;
	private JScrollPane annotationButtonsScrollPane;
	private A currentlySelectedRow;
	private Set< String > annotationNames;
	private String objectName = "Entity";
	private ArrayList< A > annotations;

	public AnnotationUI( String columnName, AnnotationTableModel< A > tableModel, SelectionModel< A > selectionModel, RowSorter< ? extends TableModel > rowSorter )
	{
		super("");
		this.annotationColumnName = columnName;
		this.annotationNames = new HashSet<>();
		this.tableModel = tableModel;
		this.selectionModel = selectionModel;
		this.rowSorter = rowSorter;
		this.currentlySelectedRow = tableModel.annotation( rowSorter.convertRowIndexToModel( 0 ) );
		if (  tableModel.annotation( 0 ) instanceof AnnotatedRegion )
		{
			// automatically select the first region
			selectionModel.setSelected( currentlySelectedRow, true );
		}

		configureAnnotatedObjectType( tableModel );
		selectionModel.listeners().add( this );

		if ( ! columnNameToColoringModel.containsKey( annotationColumnName ) )
		{
			final CategoricalAnnotationColoringModel< A > categoricalColoringModel =  ColoringModels.createCategoricalModel( annotationColumnName, GLASBEY, DARK_GREY );
			columnNameToColoringModel.put( annotationColumnName, categoricalColoringModel );
		}

		this.coloringModel = columnNameToColoringModel.get( annotationColumnName );

		annotations = tableModel.annotations();
		this.panel = new JPanel();
	}

	public CategoricalAnnotationColoringModel< A > getColoringModel()
	{
		return coloringModel;
	}

	private void configureAnnotatedObjectType( AnnotationTableModel< A > tableModel )
	{
		if ( tableModel.annotation( 0 ) instanceof AnnotatedSegment )
		{
			objectName = "Segment";
		}
		else if ( tableModel.annotation( 0 ) instanceof AnnotatedSpot )
		{
			objectName = "Spot";
		}
		else
		{
			objectName = "Region";
		}
	}

	public void showDialog()
	{
		MoBIELaf.MoBIELafOn();
		createDialog();
		showFrame();
		MoBIELaf.MoBIELafOff();
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
		final JPanel panel = SwingHelper.horizontalBoxLayoutPanel();
		final JButton button = new JButton( "Create new category" );
		final JTextField textField = new JTextField( "nice_cell" );
		panel.add( button );
		panel.add( textField );
		button.addActionListener( e -> {
			String newClassName = textField.getText();
			if ( getAnnotations().containsKey( newClassName ) || annotationNames.contains( newClassName )  )
			{
				IJ.error( "Category " + newClassName + " exists already." );
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

		final JPanel panel = SwingHelper.horizontalBoxLayoutPanel();
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
		final Map< String, A > annotations = getAnnotations();
		for ( Map.Entry< String, A > entry : annotations.entrySet() )
		{
			addAnnotationButtonPanel( entry.getKey(), entry.getValue() );
		}
	}

	private void addAnnotationButtonPanel( String annotationName, A annotation )
	{
		MoBIELaf.MoBIELafOn();

		annotationNames.add( annotationName );

		final JPanel panel = SwingHelper.horizontalBoxLayoutPanel();
		final JButton annotateButton = new JButton( String.format("%1$15s", annotationName) );
		annotateButton.setFont( new Font("monospaced", Font.PLAIN, 12) );
		annotateButton.setOpaque( true );
		setButtonColor( annotateButton, annotation );
		annotateButton.setAlignmentX( Component.CENTER_ALIGNMENT );

		final ARGBType argbType = new ARGBType();
		coloringModel.convertStringToARGB( annotationName, argbType );
		annotateButton.setBackground( ColorHelper.getColor( argbType ) );

		annotateButton.addActionListener( e ->
		{
			if ( selectionModel.isEmpty() ) return; // nothing selected to be annotated

			final Set< A > selected = selectionModel.getSelected();

			for ( A row : selected )
			{
				row.setString( annotationColumnName, annotationName );
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
		changeColor.addActionListener( e ->
		{
			Color color = JColorChooser.showDialog( this.panel, "", null );
			if ( color == null ) return;
			annotateButton.setBackground( color );
			coloringModel.assignColor( annotationName, ColorHelper.getARGBType( color ).get() );
		} );

		panel.add( annotateButton );
		panel.add( changeColor );
		annotationButtonsContainer.add( panel );
		refreshDialog();
		MoBIELaf.MoBIELafOff();
	}

	private void addSelectPreviousAndNextPanel( )
	{
		final JPanel panel = SwingHelper.horizontalBoxLayoutPanel();
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
		final JPanel panel = SwingHelper.horizontalBoxLayoutPanel();
		final JButton button = createSelectButton();
		selectAnnotationTextField = new JTextField( "" );
		selectAnnotationTextField.setText( "1" );
		panel.add( button );
		panel.add( selectAnnotationTextField );
		this.panel.add( panel );
	}

	private JButton createSelectNextButton()
	{
		final JButton next = new JButton( "Select next" );
		//next.setFont( new Font("monospaced", Font.PLAIN, 12) );
		next.setAlignmentX( Component.CENTER_ALIGNMENT );

		next.addActionListener( e ->
		{
			// rowIndex in currently visible table
			int rowIndex = rowSorter.convertRowIndexToView( tableModel.rowIndexOf( currentlySelectedRow ) );
			final int numRows = tableModel.numAnnotations();
			if ( rowIndex < numRows - 1 )
			{
				A row = null;
				if ( skipNone )
				{
					while ( rowIndex < numRows - 1 )
					{
						row = tableModel.annotation( rowSorter.convertRowIndexToModel( ++rowIndex ) );
						if ( isNoneOrNan( row ) )
						{
							row = null;
							continue;
						}
						else
						{
							break;
						}
					}

					if ( row == null )
					{
						IJ.showMessage( NO_MORE_SEGMENTS );
						return; // All following rows are None or NaN
					}

				} else
				{
					row = tableModel.annotation( rowSorter.convertRowIndexToModel( ++rowIndex ) );
				}
				selectRow( row );
			}
			else
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
			int rowIndex = rowSorter.convertRowIndexToView( tableModel.rowIndexOf( currentlySelectedRow ) );
			if ( rowIndex > 0 )
			{
				A row = null;
				if ( skipNone )
				{
					while ( rowIndex > 0 )
					{
						row = tableModel.annotation( rowSorter.convertRowIndexToModel( --rowIndex ) );
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
					row = tableModel.annotation( rowSorter.convertRowIndexToModel( --rowIndex ) );
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
			A selectedRow = fetchManuallySelectedAnnotation();
			if ( selectedRow != null ) selectRow( selectedRow );
		} );
		return button;
	}

	private A fetchManuallySelectedAnnotation()
	{
		final String annotationId = selectAnnotationTextField.getText();

		List< A > selectedAnnotations;
		try
		{
			// For spots and segments the entered ID may be just the
			// label id; as this is a frequently occurring use-case, try this first.
			final int labelId = Integer.parseInt( annotationId );
			selectedAnnotations = annotations.parallelStream().filter( a -> a.label() == labelId ).collect( Collectors.toList() );
		}
		catch ( Exception e )
		{
			// Match the full UUID
			selectedAnnotations = annotations.parallelStream().filter( a -> a.uuid().equals( annotationId ) ).collect( Collectors.toList() );
		}

		if ( selectedAnnotations.size() == 0 )
			throw new RuntimeException( "Could not find " + annotationId );

		if ( selectedAnnotations.size() > 1 )
			throw new RuntimeException( "Found multiple matches of " + annotationId );

		return selectedAnnotations.get( 0 );
	}

	private boolean isNoneOrNan( A row )
	{
		// TODO: I think the default for None is an empty String in TableSaw
		return ( ( String ) row.getValue( annotationColumnName ) ).equalsIgnoreCase( DefaultValues.NONE );
	}

	private void selectRow( A row )
	{
		currentlySelectedRow = row;
		selectionModel.clearSelection();
		selectionModel.setSelected( row, true );
		selectionModel.focus( row, this );
	}

	private void addSkipNonePanel( )
	{
		final JPanel panel = SwingHelper.horizontalBoxLayoutPanel();

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
		final JPanel panel = SwingHelper.horizontalBoxLayoutPanel();

		final JCheckBox checkBox = new JCheckBox( "Keep "+ objectName +"(s) selected after assignment" );
		checkBox.setSelected( false );
		isKeepSelectedMode = checkBox.isSelected();

		checkBox.addActionListener( e -> {
			isKeepSelectedMode = checkBox.isSelected();
		}  );

		panel.add( checkBox );
		this.panel.add( panel );
	}

	private void setButtonColor( JButton button, A annotation )
	{
		if ( annotation != null )
		{
			final ARGBType argbType = new ARGBType();
			coloringModel.convert( annotation, argbType );
			button.setBackground( new Color( argbType.get() ) );
		}
	}

	private Map< String, A > getAnnotations()
	{
		// Create a map with the current annotation values
		// and one ("representative") annotation object.
		// This object is needed to determine the coloring.
		final Map< String, A > map = new HashMap<>();
		for ( A annotation : annotations )
			map.put( (String) annotation.getValue( annotationColumnName ), annotation );
		return map;
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
	public void focusEvent( A selection, Object initiator )
	{
		currentlySelectedRow = selection;
	}
}
