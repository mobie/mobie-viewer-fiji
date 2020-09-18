package de.embl.cba.mobie.image;

import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.tables.color.ColorUtils;
import net.imglib2.type.numeric.ARGBType;
import org.apache.commons.lang.WordUtils;

import static de.embl.cba.mobie.utils.Utils.createRandom;

public class ImagePropertiesToMetadataAdapter
{
	public void setMetadata( Metadata metadata, MutableImageProperties imageProperties )
	{
		metadata.contrastLimits = imageProperties.contrastLimits != null
				? imageProperties.contrastLimits : metadata.contrastLimits;
		metadata.color = imageProperties.color != null
				? imageProperties.color : metadata.color;

		if ( metadata.color != null )
		{
			// within MoBIE the convention is Captial first letter:
			// see class ColoringLuts
			metadata.color = WordUtils.capitalize( metadata.color );
		}

		metadata.colorByColumn = imageProperties.colorByColumn;
		metadata.selectedSegmentIds = imageProperties.selectedLabelIds;
		metadata.showImageIn3d = imageProperties.showImageIn3d;
		metadata.showSelectedSegmentsIn3d = imageProperties.showSelectedSegmentsIn3d;
		metadata.additionalSegmentTableNames = imageProperties.tables;
	}
}
