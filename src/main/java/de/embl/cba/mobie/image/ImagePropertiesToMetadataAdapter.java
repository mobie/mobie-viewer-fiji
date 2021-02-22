package de.embl.cba.mobie.image;

import de.embl.cba.bdv.utils.sources.Metadata;
import org.apache.commons.lang.WordUtils;

import java.util.ArrayList;

public class ImagePropertiesToMetadataAdapter
{
	public void setMetadataFromMutableImageProperties(Metadata metadata, MutableImageProperties imageProperties )
	{
		metadata.contrastLimits = imageProperties.contrastLimits != null
				? imageProperties.contrastLimits : metadata.contrastLimits;
		metadata.color = imageProperties.color != null
				? imageProperties.color : metadata.color;
		metadata.valueLimits = imageProperties.valueLimits != null
				? imageProperties.valueLimits : metadata.valueLimits;
		metadata.resolution3dView = imageProperties.resolution3dView;

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
		metadata.addedTransform = imageProperties.addedTransform;
	}

	public void setMutableImagePropertiesFromMetadata( MutableImageProperties imageProperties, Metadata metadata ) {
		imageProperties.contrastLimits = metadata.contrastLimits != null
				? metadata.contrastLimits : imageProperties.contrastLimits;
		imageProperties.color = metadata.color != null
				? metadata.color : imageProperties.color;
		imageProperties.valueLimits = metadata.valueLimits != null
				? metadata.valueLimits : imageProperties.valueLimits;
		imageProperties.addedTransform = metadata.addedTransform != null
				? metadata.addedTransform : imageProperties.addedTransform;

		imageProperties.resolution3dView = metadata.resolution3dView;
		imageProperties.colorByColumn = metadata.colorByColumn;
		imageProperties.selectedLabelIds = metadata.selectedSegmentIds != null
				? new ArrayList<>(metadata.selectedSegmentIds) : imageProperties.selectedLabelIds;
		imageProperties.showImageIn3d = metadata.showImageIn3d;
		imageProperties.showSelectedSegmentsIn3d = metadata.showSelectedSegmentsIn3d;
		imageProperties.tables = metadata.additionalSegmentTableNames != null
				? new ArrayList<>(metadata.additionalSegmentTableNames): imageProperties.tables;
	}
}
