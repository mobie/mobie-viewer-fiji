package org.embl.mobie.lib.plot;

import java.util.List;

public class ScatterPlotSettings
{
	public String[] selectedColumns;
	public boolean invertY = true;
	public double aspectRatio = 0.0;
	public double dotSize = 5.0;
	public boolean showAllTimepoints = true;

	public ScatterPlotSettings( String[] selectedColumns )
	{
		this.selectedColumns = selectedColumns;
	}
}
