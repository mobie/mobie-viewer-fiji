package de.embl.cba.platynereis.platybrowser;


import java.util.HashMap;

/**
 * TODO: probably read this from a editable text file so that users can add own views.
 *
 *
 */
public class PlatyViews
{
	public static final String LEFT_EYE_POSITION = "Left eye";
	private final HashMap< String, double[] > nameToView;

	public PlatyViews()
	{
		nameToView = new HashMap<>();
		nameToView.put( LEFT_EYE_POSITION, new double[]{177,218,67,0} );
	}

	public HashMap< String, double[] > views()
	{
		return nameToView;
	}

}
