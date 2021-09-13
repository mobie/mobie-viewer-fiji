package org.embl.mobie.viewer.color;

import de.embl.cba.bdv.utils.lut.ARGBLut;
import de.embl.cba.bdv.utils.lut.BlueWhiteRedARGBLut;
import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.bdv.utils.lut.ViridisARGBLut;
import de.embl.cba.tables.color.ColoringLuts;

// TODO:
//  Make the LUTs an inner enum class of the LutFactory
public class LutFactory
{
	public ARGBLut get( String lut )
	{
		switch ( lut )
		{
			case ColoringLuts.BLUE_WHITE_RED:
				return new BlueWhiteRedARGBLut( 1000 );
			case ColoringLuts.VIRIDIS:
				return new ViridisARGBLut();
			case ColoringLuts.GLASBEY:
				return new GlasbeyARGBLut();
			default:
				return new GlasbeyARGBLut();
		}
	}
}
