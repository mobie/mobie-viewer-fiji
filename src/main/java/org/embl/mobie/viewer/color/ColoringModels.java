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
package org.embl.mobie.viewer.color;

import org.embl.mobie.viewer.annotation.Annotation;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Pair;

import javax.annotation.Nullable;
import javax.swing.*;

public class ColoringModels
{
	public static < A extends Annotation > void setColorsFromColumn( String selectedColumnName, CategoricalAnnotationColoringModel< A > coloringModel )
	{
		// TODO
//		int rowCount = tableModel.numRows();
//
//		for ( int i = 0; i < rowCount; i++)
//		{
//			String argbString = (String) tableModel.row( i ).getValue( selectedColumnName );
//
//			if ( !argbString.equals("NaN") & !argbString.equals("None") ) {
//				String[] splitArgbString = argbString.split("-");
//
//				int[] argbValues = new int[4];
//				for (int j = 0; j < splitArgbString.length; j++) {
//					argbValues[j] = Integer.parseInt(splitArgbString[j]);
//				}
//
//				coloringModel.assignColor(argbString, new ARGBType(ARGBType.rgba(argbValues[1], argbValues[2], argbValues[3], argbValues[0])));
//			}
//		}
	}

	public static < A extends Annotation > CategoricalAnnotationColoringModel< A > createCategoricalModel(
			String columnName, @Nullable
			String lutName,
			ARGBType colorForNoneOrNaN )
	{
		final CategoricalAnnotationColoringModel< A > coloringModel
				= new CategoricalAnnotationColoringModel<>(
						columnName,
						lutName );

		coloringModel.assignColor( "Infinity", colorForNoneOrNaN.get() );
		coloringModel.assignColor( "NaN", colorForNoneOrNaN.get() );
		coloringModel.assignColor( "None", colorForNoneOrNaN.get() );

		return coloringModel;
	}

	public static < A extends Annotation > NumericAnnotationColoringModel< A > createNumericModel(
			String columnName,
			String lutName,
			Pair< Double, Double > contrastLimits,
			boolean showUI
	)
	{
		final NumericAnnotationColoringModel< A > coloringModel
				= new NumericAnnotationColoringModel<>(
						columnName,
						lutName,
						contrastLimits );

		if ( showUI )
			SwingUtilities.invokeLater( () ->
				new NumericColoringModelDialog( columnName, coloringModel ) );

		return coloringModel;
	}
}
