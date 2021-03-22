package de.embl.cba.mobie2;

public class SourceDisplaySupplier
{
	public ImageDisplays imageDisplays;
	public SegmentationDisplays segmentationDisplays;

	public SourceDisplays getSourceDisplay()
	{
		if ( imageDisplays != null ) return imageDisplays;
		else if ( segmentationDisplays != null ) return segmentationDisplays;
		else throw new RuntimeException( "Unsupported SourceDisplay" );
	}
}
