package de.embl.cba.platynereis.image;

import de.embl.cba.bdv.utils.sources.Metadata;

public class ImagePropertiesToMetadataAdapter
{
	public void setMetadata( Metadata metadata, MutableImageProperties imageProperties )
	{
		metadata.contrastLimits = imageProperties.contrastLimits != null
				? metadata.contrastLimits : imageProperties.contrastLimits;
		metadata.color = imageProperties.color != null
				? metadata.color : imageProperties.color;

		metadata.colorByColumn = imageProperties.colorByColumn;
		metadata.selectedSegmentIds = imageProperties.selectedLabelIds;
		metadata.showImageIn3d = imageProperties.showImageIn3d;
		metadata.showSelectedSegmentsIn3d = imageProperties.showSelectedSegmentsIn3d;
		metadata.additionalSegmentTableNames = imageProperties.tables;
	}
}
