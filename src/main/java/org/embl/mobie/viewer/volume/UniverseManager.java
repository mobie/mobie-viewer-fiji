package org.embl.mobie.viewer.volume;

import ij3d.Image3DUniverse;

public class UniverseManager
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

	public void close()
	{
		if ( universe != null )
		{
			universe.close();
		}
	}

}
