/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
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
package org.embl.mobie.lib.ui;

import ij.gui.GenericDialog;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.color.ColoringModel;
import org.embl.mobie.lib.color.ColoringModels;
import org.embl.mobie.lib.color.lut.LUTs;
import org.embl.mobie.lib.table.AnnotationTableModel;
import net.imglib2.util.Pair;

import java.util.List;

import static org.embl.mobie.lib.color.lut.LUTs.COLORING_LUTS;
import static org.embl.mobie.lib.color.lut.LUTs.TRANSPARENT;

public class ColumnColoringModelDialog< A extends Annotation>
{
	private static String lut;
	private static String columnName;
	private static boolean paintZeroTransparent;
	private AnnotationTableModel< A > table;

	public ColumnColoringModelDialog( AnnotationTableModel< A > table )
	{
		this.table = table;
	}

	public ColoringModel< A > showDialog( )
	{
		final List< String > columnNames = table.columnNames();
		final String[] columnNameArray = columnNames.toArray( new String[ 0 ] );
		final GenericDialog gd = new GenericDialog( "Color by Column" );
		if ( columnName == null || ! columnNames.contains( columnName ) ) columnName = columnNameArray[ 0 ];
		gd.addChoice( "Column", columnNameArray, columnName );

		if ( lut == null ) lut = COLORING_LUTS[ 0 ];
		gd.addChoice( "Coloring", COLORING_LUTS, lut );

		gd.addCheckbox( "Paint Zero Transparent", paintZeroTransparent );

		gd.showDialog();
		if ( gd.wasCanceled() ) return null;

		columnName = gd.getNextChoice();
		lut = gd.getNextChoice();
		paintZeroTransparent = gd.getNextBoolean();

		if ( paintZeroTransparent )
			lut += LUTs.ZERO_TRANSPARENT;

		if ( LUTs.isNumeric( lut ) )
		{
			final Pair< Double, Double > minMax = table.getMinMax( columnName );
			return ColoringModels.createNumericModel( columnName, lut, minMax, true );
		}
		else if ( LUTs.isCategorical( lut ) )
		{
			return ColoringModels.createCategoricalModel( columnName, lut, TRANSPARENT );
		}
		else
		{
			throw new UnsupportedOperationException( "LUT " + lut + " is not supported." );
		}
	}
}
