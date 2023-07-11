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
package org.embl.mobie.lib.plot;

import ij.gui.GenericDialog;

public class ScatterPlotDialog
{
	private final String[] columns;
	private final ScatterPlotSettings settings;

	public ScatterPlotDialog( String[] columns, ScatterPlotSettings settings )
	{
		this.columns = columns;
		this.settings = settings;
	}

	public boolean show()
	{
		final String[] xy = { "X", "Y " };

		// lineChoices = new String[]{ GridLinesOverlay.NONE, GridLinesOverlay.Y_NX, GridLinesOverlay.Y_N };

		final GenericDialog gd = new GenericDialog( "Scatter Plot Configuration" );

		gd.addCheckbox( "Plot All Timepoints at Once", settings.showAllTimepoints );

		for ( int d = 0; d < 2; d++ )
		{
			gd.addChoice( "Column " + xy[ d ], columns, settings.selectedColumns[ d ] );
		}

		gd.addNumericField( "Aspect Ratio (0 = Auto)", settings.aspectRatio );
		gd.addNumericField( "Dot Size", settings.dotSize );
		gd.showDialog();

		if ( gd.wasCanceled() ) return false;

		for ( int d = 0; d < xy.length; d++ )
			settings.selectedColumns[ d ] = gd.getNextChoice();
		settings.aspectRatio = gd.getNextNumber();
		settings.showAllTimepoints = gd.getNextBoolean();
		settings.dotSize = gd.getNextNumber();

		return true;
	}

	public ScatterPlotSettings getSettings()
	{
		return settings;
	}
}
