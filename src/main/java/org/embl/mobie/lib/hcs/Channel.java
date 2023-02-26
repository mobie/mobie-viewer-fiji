package org.embl.mobie.lib.hcs;

public class Channel
{
	private final String name;
	private String color;
	private double[] contrastLimits;

	public Channel( String name )
	{
		this.name = name;
	}

	public String getColor()
	{
		return color;
	}

	public void setColor( String color )
	{
		this.color = color;
	}

	public double[] getContrastLimits()
	{
		return contrastLimits;
	}

	public void setContrastLimits( double[] contrastLimits )
	{
		this.contrastLimits = contrastLimits;
	}

	public String getName()
	{
		return name;
	}
}
