package de.embl.cba.mobie2.color;

import de.embl.cba.bdv.utils.lut.ARGBLut;
import de.embl.cba.bdv.utils.lut.BlueWhiteRedARGBLut;
import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.bdv.utils.lut.IndexARGBLut;
import de.embl.cba.bdv.utils.lut.ViridisARGBLut;
import de.embl.cba.tables.color.ColoringLuts;

import static de.embl.cba.tables.color.CategoryTableRowColumnColoringModel.TRANSPARENT;

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
