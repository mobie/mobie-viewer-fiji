package de.embl.cba.mobie2.color;

import de.embl.cba.mobie2.display.SegmentationSourceDisplay;
import de.embl.cba.tables.color.ColoringLuts;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.color.ColumnColoringModelCreator;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import org.apache.commons.lang.WordUtils;

public class ColoringModelHelper
{
	public static void configureMoBIEColoringModel( SegmentationSourceDisplay segmentationDisplay )
	{
		if ( segmentationDisplay.getColorByColumn() != null )
		{
			final ColumnColoringModelCreator< TableRowImageSegment > modelCreator = new ColumnColoringModelCreator( segmentationDisplay.segments );

			final ColoringModel< TableRowImageSegment > coloringModel;

			String coloringLut = getColoringLut( segmentationDisplay );

			coloringModel = modelCreator.createColoringModel( segmentationDisplay.getColorByColumn(), coloringLut, segmentationDisplay.getValueLimits()[ 0 ], segmentationDisplay.getValueLimits()[ 1 ] );

			segmentationDisplay.coloringModel = new MoBIEColoringModel( coloringModel );
		}
		else
		{
			segmentationDisplay.coloringModel = new MoBIEColoringModel<>( segmentationDisplay.getLut() );
		}
	}

	private static String getColoringLut( SegmentationSourceDisplay segmentationDisplay )
	{
		String coloringLut = segmentationDisplay.getLut() ;
		if ( coloringLut.equals( "argbColumn" ) )
			coloringLut = ColoringLuts.ARGB_COLUMN;
		else
			coloringLut = WordUtils.capitalize( coloringLut );
		return coloringLut;
	}
}
