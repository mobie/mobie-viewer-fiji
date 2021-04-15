package de.embl.cba.mobie2.display;

public class SourceDisplaySupplier
{
	// TODO: this could be handled during deserialization
	private ImageDisplay imageDisplay;
	private SegmentationDisplay segmentationDisplay;

	public Display get()
	{
		if ( imageDisplay != null ) return imageDisplay;
		else if ( segmentationDisplay != null ) return segmentationDisplay;
		else throw new RuntimeException( "No SourceDisplay set" );
	}
}
