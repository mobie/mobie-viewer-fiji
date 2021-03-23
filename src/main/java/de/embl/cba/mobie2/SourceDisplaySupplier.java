package de.embl.cba.mobie2;

public class SourceDisplaySupplier
{
	private ImageDisplays imageDisplays;
	private SegmentationDisplays segmentationDisplays;

	public SourceDisplays get()
	{
		if ( imageDisplays != null ) return imageDisplays;
		else if ( segmentationDisplays != null ) return segmentationDisplays;
		else throw new RuntimeException( "Unsupported SourceDisplay" );
	}
}
