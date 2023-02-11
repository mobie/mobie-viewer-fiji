package org.embl.mobie.lib.plot;

public class ScatterPlotSettings
{
	public String[] columns;
	public boolean invertY = true;
	public double aspectRatio = -1.0;
	public double dotSize = 1.0;
	public boolean showAllTimepoints = true;

	public ScatterPlotSettings( String[] columns )
	{
		this.columns = columns;
	}
}
