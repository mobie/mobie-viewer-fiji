package org.embl.mobie.lib.plot;

import java.util.List;

public class ScatterPlotSettings
{
	public String[] availableColumns;
	public String[] selectedColumns;
	public boolean invertY = true;
	public double aspectRatio = -1.0;
	public double dotSize = 1.0;
	public boolean showAllTimepoints = true;

	public ScatterPlotSettings( String[] selectedColumns, List< String > strings )
	{
		this.selectedColumns = selectedColumns;
	}
}
