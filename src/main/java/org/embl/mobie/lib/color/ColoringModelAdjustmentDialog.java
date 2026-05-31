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
package org.embl.mobie.lib.color;

import ij.IJ;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.table.AnnotationTableModel;
import org.embl.mobie.lib.select.SelectionModel;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class ColoringModelAdjustmentDialog extends JFrame
{
	private final ColoringModel< ? > coloringModel;
	private final SelectionModel< ? > selectionModel;
	private final AnnotationTableModel< ? > tableModel;

	public ColoringModelAdjustmentDialog( ColoringModel< ? > coloringModel )
	{
		this( coloringModel, null, null );
	}

	public ColoringModelAdjustmentDialog( ColoringModel< ? > coloringModel,
										  SelectionModel< ? > selectionModel,
										  AnnotationTableModel< ? > tableModel )
	{
		this.coloringModel = coloringModel;
		this.selectionModel = selectionModel;
		this.tableModel = tableModel;
		setTitle( "Coloring (And Selection) Adjustment" );
		setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		setBounds( MouseInfo.getPointerInfo().getLocation().x,
				MouseInfo.getPointerInfo().getLocation().y,
				360, 480 );
		refresh();

		setVisible( coloringModel );
	}

	public void refresh()
	{
		final JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.PAGE_AXIS ) );

		if ( coloringModel instanceof AdditiveColoringModel )
		{
			final AdditiveColoringModel< ? > model = ( AdditiveColoringModel< ? > ) coloringModel;
			for ( AdditiveColoringModel.Entry< ? > entry : model.getEntries() )
				panel.add( createEntryPanel( model, entry ) );
		}
		else
		{
			panel.add( createSingleModelPanel( coloringModel ) );
		}

		setContentPane( new JScrollPane( panel ) );

		// Add selection buttons at the bottom only if a selection model and table model were provided
		if ( selectionModel != null && tableModel != null )
		{
			final JPanel bottom = new JPanel( new FlowLayout( FlowLayout.CENTER ) );
			// thin divider line above the buttons to improve UI
			bottom.setBorder( BorderFactory.createMatteBorder( 1, 0, 0, 0, Color.LIGHT_GRAY ) );

			// If there is more than one coloring model shown (additive with multiple entries)
			// provide two buttons for creating selections with OR / AND semantics.
			boolean multipleModelsShown = false;
			if ( coloringModel instanceof AdditiveColoringModel )
			{
				multipleModelsShown = ( ( AdditiveColoringModel< ? > ) coloringModel ).getEntries().size() > 1;
			}

			if ( multipleModelsShown )
			{
				final JButton orButton = new JButton( "Create Selection [OR]" );
				orButton.setToolTipText( "Select annotations that match any of the numeric colorings (logical OR)" );
				orButton.addActionListener( e -> SwingUtilities.invokeLater( () -> selectCorrespondingAnnotations( false ) ) );
				bottom.add( orButton );

				final JButton andButton = new JButton( "Create Selection [AND]" );
				andButton.setToolTipText( "Select annotations that match all of the numeric colorings (logical AND)" );
				andButton.addActionListener( e -> SwingUtilities.invokeLater( () -> selectCorrespondingAnnotations( true ) ) );
				bottom.add( andButton );
			}
			else
			{
				final JButton selectAnnotationsButton = new JButton( "Select Corresponding Annotations" );
				selectAnnotationsButton.setToolTipText( "Select annotations that match the numeric ranges of the numeric colorings" );
				selectAnnotationsButton.addActionListener( e -> SwingUtilities.invokeLater( () -> selectCorrespondingAnnotations( false ) ) );
				bottom.add( selectAnnotationsButton );
			}

			// Clear selection button is always shown when a selectionModel/tableModel is present
			final JButton clearSelectionButton = new JButton( "Clear Selection" );
			clearSelectionButton.setToolTipText( "Clear all selections in the associated table" );
			clearSelectionButton.addActionListener( e -> SwingUtilities.invokeLater( () -> {
				selectionModel.clearSelection();
				IJ.log( "Cleared selection in the associated table." );
			} ) );
			bottom.add( clearSelectionButton );

			panel.add( bottom );
		}
		pack();
		revalidate();
		repaint();
		setVisible( coloringModel );
	}

	private void setVisible( ColoringModel< ? > coloringModel )
	{
		// only show if there is at least one coloring model
		// that has adjustments

		if ( coloringModel instanceof CategoricalAnnotationColoringModel )
		{
			setVisible( false );
		}
		else if ( coloringModel instanceof AdditiveColoringModel )
		{
			boolean allCategorical = ( ( AdditiveColoringModel< ? > ) coloringModel ).getColoringModels()
					.stream().allMatch( cm -> cm instanceof CategoricalAnnotationColoringModel );
			setVisible( ! allCategorical );
		}
		else
		{
			setVisible( true );
		}
	}

	private JPanel createEntryPanel( AdditiveColoringModel< ? > additive, AdditiveColoringModel.Entry< ? > entry )
	{
		final JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.PAGE_AXIS ) );
		panel.setBorder( BorderFactory.createEmptyBorder( 4, 4, 4, 4 ) );

		final JPanel header = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
		final JCheckBox enabledCheckBox = new JCheckBox();
		enabledCheckBox.setSelected( entry.isEnabled() );
		enabledCheckBox.addActionListener( e -> setEntryEnabled( additive, entry, enabledCheckBox.isSelected() ) );
		header.add( enabledCheckBox );
		header.add( new JLabel( entry.getName() ) );
		panel.add( header );

		panel.add( createContrastComponent( entry.getColoringModel() ) );

		return panel;
	}

	private JPanel createSingleModelPanel( ColoringModel< ? > model )
	{
		final JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.PAGE_AXIS ) );
		panel.setBorder( BorderFactory.createEmptyBorder( 4, 4, 4, 4 ) );

		final JPanel header = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
		header.add( new JLabel( ColoringModels.getName( model ) ) );
		panel.add( header );

		panel.add( createContrastComponent( model ) );

		return panel;
	}

	private JComponent createContrastComponent( ColoringModel< ? > model )
	{
		if ( model instanceof NumericAnnotationColoringModel )
		{
			return new NumericAnnotationColoringModelContrastPanel( ( NumericAnnotationColoringModel< ? > ) model );
		}
		final JPanel wrapper = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
		final JLabel label = new JLabel( "No contrast controls available." );
		label.setBorder( BorderFactory.createEmptyBorder( 0, 24, 0, 0 ) );
		wrapper.add( label );
		return wrapper;
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	private void setEntryEnabled( AdditiveColoringModel< ? > additive, AdditiveColoringModel.Entry< ? > entry, boolean enabled )
	{
		( ( AdditiveColoringModel ) additive ).setEnabled( entry, enabled );
	}

	/**
	 * Find numeric coloring models represented in this dialog (either single or entries
	 * of an additive coloring) and select matching annotations in any open table
	 * views by setting the corresponding display selection model.
	 */
	@SuppressWarnings( "unchecked" )
	private void selectCorrespondingAnnotations( boolean requireAll )
	{
		if ( selectionModel == null )
		{
			JOptionPane.showMessageDialog( this, "No selection model is associated with this dialog." );
			return;
		}

		final List< NumericAnnotationColoringModel< ? > > numericModels = new ArrayList<>();

		if ( coloringModel instanceof NumericAnnotationColoringModel )
			numericModels.add( ( NumericAnnotationColoringModel< ? > ) coloringModel );
		else if ( coloringModel instanceof AdditiveColoringModel )
		{
			final AdditiveColoringModel< ? > additive = ( AdditiveColoringModel< ? > ) coloringModel;
			for ( AdditiveColoringModel.Entry< ? > entry : additive.getEntries() )
			{
				final ColoringModel< ? > cm = entry.getColoringModel();
				if ( cm instanceof NumericAnnotationColoringModel )
					numericModels.add( ( NumericAnnotationColoringModel< ? > ) cm );
			}
		}

		if ( numericModels.isEmpty() )
		{
			JOptionPane.showMessageDialog( this, "No numeric colorings available to select." );
			return;
		}

		final List< ? > annotations = tableModel.annotations();

		final List< Object > matches = new ArrayList<>();

		for ( Object o : annotations )
		{
			if ( ! ( o instanceof Annotation ) ) continue;
			final Annotation a = ( Annotation ) o;

			if ( ! requireAll )
			{
				for ( NumericAnnotationColoringModel< ? > nm : numericModels )
				{
					final String columnName = nm.getColumnName();
					if ( columnName == null ) continue;
					final Number number = a.getNumber( columnName );
					if ( number == null ) continue;
					double v = number.doubleValue();
					if ( Double.isFinite( v ) && v >= nm.getMin() && v <= nm.getMax() )
					{
						matches.add( a );
						break; // no need to test further numeric models for this annotation
					}
				}
			}
			else
			{
				boolean allMatch = true;
				for ( NumericAnnotationColoringModel< ? > nm : numericModels )
				{
					final String columnName = nm.getColumnName();
					if ( columnName == null ) { allMatch = false; break; }
					final Number number = a.getNumber( columnName );
					if ( number == null ) { allMatch = false; break; }
					double v = number.doubleValue();
					if ( ! ( Double.isFinite( v ) && v >= nm.getMin() && v <= nm.getMax() ) )
					{
						allMatch = false;
						break;
					}
				}

				if ( allMatch )
					matches.add( a );
			}
		}

		if ( matches.isEmpty() )
		{
			JOptionPane.showMessageDialog( this, "No matching rows found for the numeric colorings" + ( requireAll ? " (AND)." : "." ) );
			return;
		}

		// Apply the selection to the provided selection model
		selectionModel.clearSelection();
		selectionModel.setSelected( ( Collection ) matches, true );
		IJ.log( "Selected " + matches.size() + " annotations in the associated table (" + ( requireAll ? "AND" : "OR" ) + " match)." );
	}
}
