package org.embl.mobie.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class ColumnValueSelectionDialog
{
	public enum SelectionMode
	{
		CREATE_NEW_SELECTION( "Create New Selection" ),
		INTERSECT_WITH_CURRENT_SELECTION( "Intersect With Current Selection" ),
		ADD_TO_CURRENT_SELECTION( "Add To Current Selection" );

		private final String label;

		SelectionMode( String label )
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	private static final int SLIDER_MAX = 1000;

	private final List< String > columnNames;
	private final Set< String > numericColumns;
	private final Function< String, double[] > numericMinMaxProvider;
	private final Function< String, List< String > > categoricalValuesProvider;

	private JComboBox< String > columnComboBox;
	private JComboBox< SelectionMode > selectionModeComboBox;
	private JPanel valueCardPanel;
	private CardLayout valueCardLayout;

	private JTextField minField;
	private JTextField maxField;
	private JSlider minSlider;
	private JSlider maxSlider;
	private double sliderColumnMin = 0.0;
	private double sliderColumnMax = 1.0;
	private double minValue = 0.0;
	private double maxValue = 1.0;
	private boolean updatingNumericControls = false;

	private DefaultListModel< String > categoricalListModel;
	private JList< String > categoricalValueList;
	private JTextField categoricalTextField;

	private boolean okPressed;

	public ColumnValueSelectionDialog(
			List< String > columnNames,
			List< String > numericColumnNames,
			Function< String, double[] > numericMinMaxProvider,
			Function< String, List< String > > categoricalValuesProvider )
	{
		this.columnNames = columnNames;
		this.numericColumns = new HashSet<>( numericColumnNames );
		this.numericMinMaxProvider = numericMinMaxProvider;
		this.categoricalValuesProvider = categoricalValuesProvider;
	}

	public boolean show()
	{
		final JDialog dialog = new JDialog( ( Frame ) null, "Select Values", true );
		dialog.setLayout( new BoxLayout( dialog.getContentPane(), BoxLayout.Y_AXIS ) );

		addColumnSelection( dialog );
		addSelectionModeSelection( dialog );
		addValuePanels( dialog );
		addOKCancelButton( dialog );

		updateValuePanelForColumn();

		dialog.setLocation(
				Toolkit.getDefaultToolkit().getScreenSize().width / 2 - 240,
				Toolkit.getDefaultToolkit().getScreenSize().height / 2 - 220
		);
		dialog.pack();
		dialog.setVisible( true );

		return okPressed;
	}

	private void addColumnSelection( JDialog dialog )
	{
		final JPanel panel = SwingHelper.horizontalFlowLayoutPanel();
		columnComboBox = new JComboBox<>( columnNames.toArray( new String[ 0 ] ) );
		columnComboBox.addActionListener( e -> updateValuePanelForColumn() );
		panel.add( new JLabel( "Column:  " ) );
		panel.add( columnComboBox );
		dialog.add( panel );
	}

	private void addSelectionModeSelection( JDialog dialog )
	{
		final JPanel panel = SwingHelper.horizontalFlowLayoutPanel();
		selectionModeComboBox = new JComboBox<>( SelectionMode.values() );
		panel.add( new JLabel( "Selection Mode:  " ) );
		panel.add( selectionModeComboBox );
		dialog.add( panel );
	}

	private void addValuePanels( JDialog dialog )
	{
		valueCardLayout = new CardLayout();
		valueCardPanel = new JPanel( valueCardLayout );
		valueCardPanel.add( createNumericPanel(), "NUMERIC" );
		valueCardPanel.add( createCategoricalPanel(), "CATEGORICAL" );
		dialog.add( valueCardPanel );
	}

	private JPanel createNumericPanel()
	{
		final JPanel panel = new JPanel( new GridBagLayout() );
		panel.setBorder( BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) );
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets( 2, 2, 2, 2 );
		constraints.anchor = GridBagConstraints.WEST;
		constraints.gridy = 0;

		addNumericPanelSpacer( panel, constraints );

		minField = new JTextField( 12 );
		minSlider = new JSlider( 0, SLIDER_MAX, 0 );
		minSlider.setPreferredSize( new Dimension( 220, 24 ) );
		minSlider.addChangeListener( e ->
		{
			if ( updatingNumericControls ) return;
			final double sliderValue = sliderToValue( minSlider.getValue() );
			minValue = Math.min( sliderValue, maxValue );
			updateNumericControlsFromValues();
		} );
		minField.addActionListener( e -> commitNumericFieldValues() );
		constraints.gridy++;
		addNumericControlRow( panel, constraints, "Min:  ", minField, minSlider );

		constraints.gridy++;
		addNumericPanelSpacer( panel, constraints );

		maxField = new JTextField( 12 );
		maxSlider = new JSlider( 0, SLIDER_MAX, SLIDER_MAX );
		maxSlider.setPreferredSize( new Dimension( 220, 24 ) );
		maxSlider.addChangeListener( e ->
		{
			if ( updatingNumericControls ) return;
			final double sliderValue = sliderToValue( maxSlider.getValue() );
			maxValue = Math.max( sliderValue, minValue );
			updateNumericControlsFromValues();
		} );
		maxField.addActionListener( e -> commitNumericFieldValues() );
		constraints.gridy++;
		addNumericControlRow( panel, constraints, "Max:  ", maxField, maxSlider );

		constraints.gridy++;
		constraints.gridx = 0;
		constraints.weighty = 1.0;
		constraints.fill = GridBagConstraints.VERTICAL;
		panel.add( Box.createVerticalGlue(), constraints );

		return panel;
	}

	private void addNumericPanelSpacer( JPanel panel, GridBagConstraints constraints )
	{
		constraints.gridx = 0;
		constraints.gridwidth = 3;
		constraints.weightx = 1.0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		panel.add( Box.createVerticalStrut( 12 ), constraints );
		constraints.gridwidth = 1;
	}

	private void addNumericControlRow(
			JPanel panel,
			GridBagConstraints constraints,
			String label,
			JTextField field,
			JSlider slider )
	{
		constraints.gridx = 0;
		constraints.weightx = 0.0;
		constraints.fill = GridBagConstraints.NONE;
		panel.add( new JLabel( label ), constraints );

		constraints.gridx = 1;
		panel.add( field, constraints );

		constraints.gridx = 2;
		constraints.weightx = 1.0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		panel.add( slider, constraints );
	}

	private JPanel createCategoricalPanel()
	{
		final JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );

//		final JPanel title = SwingHelper.horizontalFlowLayoutPanel();
//		title.add( new JLabel( "Select (Categorical) Values..." ) );
//		panel.add( title );

		categoricalListModel = new DefaultListModel<>();
		categoricalValueList = new JList<>( categoricalListModel );
		categoricalValueList.setVisibleRowCount( 8 );
		categoricalValueList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
		final JScrollPane scrollPane = new JScrollPane( categoricalValueList );
		scrollPane.setPreferredSize( new Dimension( 360, 140 ) );
		panel.add( scrollPane );

		final JPanel fallbackPanel = SwingHelper.horizontalFlowLayoutPanel();
		categoricalTextField = new JTextField( 24 );
		fallbackPanel.add( new JLabel( "Or type (;):  " ) );
		fallbackPanel.add( categoricalTextField );
		panel.add( fallbackPanel );

		return panel;
	}

	private void addOKCancelButton( JDialog dialog )
	{
		final JPanel buttonPanel = new JPanel();
		final JButton okButton = new JButton( "OK" );
		final JButton cancelButton = new JButton( "Cancel" );
		buttonPanel.add( okButton );
		buttonPanel.add( cancelButton );
		dialog.add( buttonPanel );

		okButton.addActionListener( e ->
		{
			if ( isNumericColumn( getColumnName() ) )
				commitNumericFieldValues();
			okPressed = true;
			dialog.setVisible( false );
		} );

		cancelButton.addActionListener( e ->
		{
			okPressed = false;
			dialog.setVisible( false );
		} );
	}

	private void updateValuePanelForColumn()
	{
		final String columnName = getColumnName();
		if ( columnName == null ) return;

		if ( isNumericColumn( columnName ) )
		{
			valueCardLayout.show( valueCardPanel, "NUMERIC" );
			final double[] minMax = numericMinMaxProvider.apply( columnName );
			if ( minMax != null )
			{
				sliderColumnMin = minMax[ 0 ];
				sliderColumnMax = minMax[ 1 ];
				minValue = minMax[ 0 ];
				maxValue = minMax[ 1 ];
				updateNumericControlsFromValues();
			}
		}
		else
		{
			valueCardLayout.show( valueCardPanel, "CATEGORICAL" );
			categoricalListModel.clear();
			for ( String value : categoricalValuesProvider.apply( columnName ) )
				categoricalListModel.addElement( value );
			categoricalValueList.clearSelection();
			categoricalTextField.setText( "" );
		}
	}

	private boolean isNumericColumn( String columnName )
	{
		return numericColumns.contains( columnName );
	}

	private void updateNumericControlsFromValues()
	{
		updatingNumericControls = true;

		minField.setText( formatDouble( minValue ) );
		maxField.setText( formatDouble( maxValue ) );
		minSlider.setValue( valueToSlider( minValue ) );
		maxSlider.setValue( valueToSlider( maxValue ) );

		updatingNumericControls = false;
	}

	private void commitNumericFieldValues()
	{
		try
		{
			final double parsedMin = Double.parseDouble( minField.getText().trim() );
			final double parsedMax = Double.parseDouble( maxField.getText().trim() );
			minValue = clamp( Math.min( parsedMin, parsedMax ), sliderColumnMin, sliderColumnMax );
			maxValue = clamp( Math.max( parsedMin, parsedMax ), sliderColumnMin, sliderColumnMax );
			updateNumericControlsFromValues();
		}
		catch ( Exception ignored )
		{
			updateNumericControlsFromValues();
		}
	}

	private double sliderToValue( int sliderValue )
	{
		final double fraction = sliderValue / ( double ) SLIDER_MAX;
		return sliderColumnMin + fraction * ( sliderColumnMax - sliderColumnMin );
	}

	private int valueToSlider( double value )
	{
		if ( sliderColumnMax == sliderColumnMin ) return 0;
		final double fraction = ( value - sliderColumnMin ) / ( sliderColumnMax - sliderColumnMin );
		return ( int ) Math.round( clamp( fraction, 0.0, 1.0 ) * SLIDER_MAX );
	}

	private double clamp( double value, double min, double max )
	{
		return Math.max( min, Math.min( max, value ) );
	}

	private String formatDouble( double value )
	{
		return Double.toString( value );
	}

	public String getColumnName()
	{
		return ( String ) columnComboBox.getSelectedItem();
	}

	public SelectionMode getSelectionMode()
	{
		return ( SelectionMode ) selectionModeComboBox.getSelectedItem();
	}

	public double getMinValue()
	{
		return minValue;
	}

	public double getMaxValue()
	{
		return maxValue;
	}

	public List< String > getSelectedCategoricalValues()
	{
		final List< String > selectedValues = new ArrayList<>( categoricalValueList.getSelectedValuesList() );
		if ( ! selectedValues.isEmpty() ) return selectedValues;

		final String typedValues = categoricalTextField.getText().trim();
		if ( typedValues.isEmpty() ) return selectedValues;

		final String[] split = typedValues.split( ";" );
		for ( String value : split )
		{
			final String trimmed = value.trim();
			if ( ! trimmed.isEmpty() )
				selectedValues.add( trimmed );
		}

		return selectedValues;
	}
}
