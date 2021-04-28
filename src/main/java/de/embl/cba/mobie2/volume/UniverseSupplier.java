package de.embl.cba.mobie2.volume;

import ij3d.Image3DUniverse;

public class UniverseSupplier
{
	private Image3DUniverse universe;

	public Image3DUniverse get()
	{
		if ( universe == null )
		{
			universe = new Image3DUniverse();
			universe.show();
		}
		return universe;
	}

	public void setUniverse( Image3DUniverse universe )
	{
		this.universe = universe;
	}
}
