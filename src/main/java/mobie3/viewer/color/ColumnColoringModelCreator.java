/*-
 * #%L
 * Various Java code for ImageJ
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
package mobie3.viewer.color;

import de.embl.cba.bdv.utils.lut.ARGBLut;
import de.embl.cba.bdv.utils.lut.BlueWhiteRedARGBLut;
import de.embl.cba.bdv.utils.lut.ColumnARGBLut;
import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.bdv.utils.lut.ViridisARGBLut;
import de.embl.cba.tables.color.ColoringLuts;
import ij.gui.GenericDialog;
import mobie3.viewer.annotation.Annotation;
import mobie3.viewer.table.AnnotationTableModel;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Pair;

import javax.annotation.Nullable;
import javax.swing.*;

import static de.embl.cba.tables.color.CategoryTableRowColumnColoringModel.TRANSPARENT;

public class ColumnColoringModelCreator< A extends Annotation >
{
	private AnnotationTableModel< A > tableModel;

	private String selectedColumnName;
	private String selectedColoringMode;
	private boolean isZeroTransparent = false;

	public static final String[] COLORING_MODES = new String[]
	{
		ColoringLuts.BLUE_WHITE_RED,
		ColoringLuts.VIRIDIS,
		ColoringLuts.GLASBEY,
		ColoringLuts.ARGB_COLUMN
	};

	// TODO: refactor into smaller classes
	public ColumnColoringModelCreator( AnnotationTableModel< A > tableModel )
	{
		this.tableModel = tableModel;
	}

	// TODO: refactor into own class
	public ColoringModel< A > showDialog()
	{
		final String[] columnNames = tableModel.columnNames().toArray( new String[ 0 ] );

		final GenericDialog gd = new GenericDialog( "Color by Column" );

		if ( selectedColumnName == null ) selectedColumnName = columnNames[ 0 ];
		gd.addChoice( "Column", columnNames, selectedColumnName );

		if ( selectedColoringMode == null ) selectedColoringMode = COLORING_MODES[ 0 ];
		gd.addChoice( "Coloring Mode", COLORING_MODES, selectedColoringMode );

		gd.addCheckbox( "Paint Zero Transparent", isZeroTransparent );

		gd.showDialog();
		if ( gd.wasCanceled() ) return null;

		selectedColumnName = gd.getNextChoice();
		selectedColoringMode = gd.getNextChoice();
		isZeroTransparent = gd.getNextBoolean();

		if ( isZeroTransparent )
			selectedColoringMode += ColoringLuts.ZERO_TRANSPARENT;

		return createColumnColoringModel( selectedColumnName, selectedColoringMode, null, null );
	}

	public ColoringModel< A > createColumnColoringModel(
			String selectedColumnName,
			String lut,
			@Nullable Pair< Double, Double > contrastLimits )
	{
		rememberChoices( selectedColumnName, lut );

		switch ( lut )
		{
			case ColoringLuts.BLUE_WHITE_RED:
				return createLinearColoringModel(
						selectedColumnName,
						lut.contains( ColoringLuts.ZERO_TRANSPARENT ),
						contrastLimits,
						new BlueWhiteRedARGBLut( 1000 ) );
			case ColoringLuts.VIRIDIS:
				return createLinearColoringModel(
						selectedColumnName,
						lut.contains( ColoringLuts.ZERO_TRANSPARENT ),
						contrastLimits,
						new ViridisARGBLut() );
			case ColoringLuts.GLASBEY:
				return createCategoricalColoringModel(
						selectedColumnName,
						lut.contains( ColoringLuts.ZERO_TRANSPARENT ),
						new GlasbeyARGBLut(),
						TRANSPARENT );
			case ColoringLuts.ARGB_COLUMN:
				return createCategoricalColoringModel(
						selectedColumnName,
						false,
						new ColumnARGBLut(),
						TRANSPARENT );
		}

		return null;
	}

	public void rememberChoices( String selectedColumnName, String selectedColoringMode )
	{
		this.selectedColumnName = selectedColumnName;
		this.selectedColoringMode = selectedColoringMode;

		if ( selectedColoringMode.contains( ColoringLuts.ZERO_TRANSPARENT ) )
			this.isZeroTransparent = true;
		else
			this.isZeroTransparent = false;
	}

	private void configureColoringModelFromARGBColumn( String selectedColumnName, CategoricalAnnotationColoringModel< A > coloringModel )
	{
		int rowCount = tableModel.numRows();

		for ( int i = 0; i < rowCount; i++)
		{
			String argbString = (String) tableModel.row( i ).getValue( selectedColumnName );

			if ( !argbString.equals("NaN") & !argbString.equals("None") ) {
				String[] splitArgbString = argbString.split("-");

				int[] argbValues = new int[4];
				for (int j = 0; j < splitArgbString.length; j++) {
					argbValues[j] = Integer.parseInt(splitArgbString[j]);
				}

				coloringModel.assignColor(argbString, new ARGBType(ARGBType.rgba(argbValues[1], argbValues[2], argbValues[3], argbValues[0])));
			}
		}
	}

	public CategoricalAnnotationColoringModel< A > createCategoricalColoringModel(
			String selectedColumnName,
			boolean isZeroTransparent,
			ARGBLut argbLut,
			ARGBType colorForNoneOrNaN )
	{
		final CategoricalAnnotationColoringModel< A > coloringModel
				= new CategoricalAnnotationColoringModel<>(
						selectedColumnName,
						argbLut );

		coloringModel.assignColor( "Infinity", colorForNoneOrNaN.get() );
		coloringModel.assignColor( "NaN", colorForNoneOrNaN.get() );
		coloringModel.assignColor( "None", colorForNoneOrNaN.get() );

		if ( isZeroTransparent )
		{
			coloringModel.assignColor( "0", TRANSPARENT.get() );
			coloringModel.assignColor( "0.0", TRANSPARENT.get() );

			if (argbLut != null) {
				argbLut.setName(argbLut.getName() + ColoringLuts.ZERO_TRANSPARENT);
			}
		}

		if ( argbLut instanceof ColumnARGBLut )
		{
			configureColoringModelFromARGBColumn( selectedColumnName, coloringModel );
		}

		return coloringModel;
	}

	private NumericAnnotationColoringModel< A > createLinearColoringModel(
			String columnName,
			boolean isZeroTransparent,
			Pair< Double, Double > contrastLimits,
			ARGBLut argbLut )
	{
		final NumericAnnotationColoringModel< A > coloringModel
				= new NumericAnnotationColoringModel<>(
						columnName,
						argbLut,
						contrastLimits,
						isZeroTransparent );

		// immediately show an UI for adjustment
		SwingUtilities.invokeLater( () ->
				new NumericColoringModelDialog( columnName, coloringModel ) );

		return coloringModel;
	}

//	private Pair< Double, Double > getValueSettings( String columnName, Pair< Double, Double > range )
//	{
//		if ( ! columnNameToRangeSettings.containsKey( columnName ) )
//			columnNameToRangeSettings.put( columnName, new ValuePair<>( range.getA(), range.getB() ) );
//
//		return columnNameToRangeSettings.get( columnName );
//	}

//	private double[] getValueRange( JTable table, String column )
//	{
//		if ( ! columnNameToMinMax.containsKey( column ) )
//		{
//			final double[] minMaxValues = Tables.minMax( column, table );
//			columnNameToMinMax.put( column, minMaxValues );
//		}
//
//		return columnNameToMinMax.get( column );
//	}

//	private double[] getValueRange( List< ? extends TableRow > tableRows, String column )
//	{
//		if ( ! columnNameToMinMax.containsKey( column ) )
//		{
//			final double[] minMaxValues = TableRows.minMax( tableRows, column );
//			columnNameToMinMax.put( column, minMaxValues );
//		}
//
//		return columnNameToMinMax.get( column );
//	}
}
