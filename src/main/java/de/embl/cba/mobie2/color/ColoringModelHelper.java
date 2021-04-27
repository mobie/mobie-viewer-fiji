package de.embl.cba.mobie2.color;

import de.embl.cba.mobie2.display.SegmentationDisplay;
import de.embl.cba.tables.color.ColoringLuts;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.color.ColumnColoringModelCreator;
import de.embl.cba.tables.color.LazyCategoryColoringModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;

public class ColoringModelHelper
{
	public static void configureMoBIEColoringModel( SegmentationDisplay segmentationDisplay )
	{
		if ( segmentationDisplay.getColorByColumn() != null )
		{
			final ColumnColoringModelCreator< TableRowImageSegment > modelCreator = new ColumnColoringModelCreator( segmentationDisplay.segments );
			final ColoringModel< TableRowImageSegment > coloringModel;

			if ( segmentationDisplay.getValueLimits() != null )
			{
				coloringModel = modelCreator.createColoringModel( segmentationDisplay.getColorByColumn(), ColoringLuts.ARGB_COLUMN, segmentationDisplay.getValueLimits()[ 0 ], segmentationDisplay.getValueLimits()[ 1 ] );
			}
			else
			{
				coloringModel = modelCreator.createColoringModel( segmentationDisplay.getColorByColumn(), ColoringLuts.ARGB_COLUMN, segmentationDisplay.getValueLimits()[ 0 ], segmentationDisplay.getValueLimits()[ 1 ] );
			}

			segmentationDisplay.coloringModel = new MoBIEColoringModel( coloringModel );
		}
		else
		{
			segmentationDisplay.coloringModel = new MoBIEColoringModel<>( segmentationDisplay.getLut() );
		}
	}
}
