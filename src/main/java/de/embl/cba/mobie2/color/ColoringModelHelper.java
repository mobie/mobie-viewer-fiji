package de.embl.cba.mobie2.color;

import de.embl.cba.mobie2.display.SegmentationDisplay;
import de.embl.cba.tables.color.ColoringLuts;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.color.ColumnColoringModelCreator;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import org.apache.commons.lang.WordUtils;

public class ColoringModelHelper
{
	public static void configureMoBIEColoringModel( SegmentationDisplay segmentationDisplay )
	{
		if ( segmentationDisplay.getColorByColumn() != null )
		{
			final ColumnColoringModelCreator< TableRowImageSegment > modelCreator = new ColumnColoringModelCreator( segmentationDisplay.segments );

			final ColoringModel< TableRowImageSegment > coloringModel;

			String coloringLut = getColoringLut( segmentationDisplay );

			Double[] valueRange = getValueRange( segmentationDisplay );

			coloringModel = modelCreator.createColoringModel( segmentationDisplay.getColorByColumn(), coloringLut, valueRange[ 0 ], valueRange[ 1 ] );

			segmentationDisplay.coloringModel = new MoBIEColoringModel( coloringModel );
		}
		else
		{
			segmentationDisplay.coloringModel = new MoBIEColoringModel<>( segmentationDisplay.getLut() );
		}
	}

	private static Double[] getValueRange( SegmentationDisplay segmentationDisplay )
	{
		Double[] minMax = new Double[]{null,null};
		if ( segmentationDisplay.getValueLimits() != null )
		{
			minMax[ 0 ] = segmentationDisplay.getValueLimits()[ 0 ];
			minMax[ 1 ] = segmentationDisplay.getValueLimits()[ 1 ];
		}
		return minMax;
	}

	private static String getColoringLut( SegmentationDisplay segmentationDisplay )
	{
		String coloringLut = segmentationDisplay.getLut() ;
		if ( coloringLut.equals( "argbColumn" ) )
			coloringLut = ColoringLuts.ARGB_COLUMN;
		else
			coloringLut = WordUtils.capitalize( coloringLut );
		return coloringLut;
	}
}
