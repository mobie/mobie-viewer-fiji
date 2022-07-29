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
package org.embl.mobie.viewer.plot;

import ij.gui.GenericDialog;

public class ScatterPlotDialog
{
	private final String[] columnNames;
	private final String[] selectedColumnNames;
	private final double[] axesScaleFactors;
	private double dotSizeScaleFactor;

	public ScatterPlotDialog( String[] columnNames, String[] selectedColumnNames, double[] axesScaleFactors, double dotSizeScaleFactor )
	{
		this.columnNames = columnNames;
		this.selectedColumnNames = selectedColumnNames;
		this.axesScaleFactors = axesScaleFactors;
		this.dotSizeScaleFactor = dotSizeScaleFactor;
	}

	public boolean show()
	{
		final String[] xy = { "X", "Y " };

		// lineChoices = new String[]{ GridLinesOverlay.NONE, GridLinesOverlay.Y_NX, GridLinesOverlay.Y_N };

		final GenericDialog gd = new GenericDialog( "Column selection" );

		for ( int d = 0; d < 2; d++ )
		{
			gd.addChoice( "Column " + xy[ d ], columnNames, selectedColumnNames[ d ] );
			gd.addNumericField( "Axis Scale Factor " + xy[ d ], axesScaleFactors[ d ] );
		}

		gd.addNumericField( "Dot Size Scale Factor", dotSizeScaleFactor );
		//gd.addChoice( "Add lines", lineChoices, GridLinesOverlay.NONE );
		gd.showDialog();

		if ( gd.wasCanceled() ) return false;

		for ( int d = 0; d < xy.length; d++ )
		{
			selectedColumnNames[ d ] = gd.getNextChoice();
			axesScaleFactors[ d ] = gd.getNextNumber();
		}
		dotSizeScaleFactor = gd.getNextNumber();

		return true;
	}

	public String[] getSelectedColumns()
	{
		return selectedColumnNames;
	}

	public double[] getAxesScaleFactors()
	{
		return axesScaleFactors;
	}

	public double getDotSizeScaleFactor()
	{
		return dotSizeScaleFactor;
	}
}
