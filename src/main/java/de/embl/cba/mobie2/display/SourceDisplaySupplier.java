package de.embl.cba.mobie2.display;

// TODO: get rid of this and handle this in a GSon adaptor
public class SourceDisplaySupplier
{
	private ImageDisplay imageDisplay;
	private SegmentationDisplay segmentationDisplay;

	public SourceDisplaySupplier( Display display ) {
		if ( display instanceof ImageDisplay ) {
			imageDisplay = (ImageDisplay) display;
		} else if ( display instanceof  SegmentationDisplay ) {
			segmentationDisplay = (SegmentationDisplay) display;
		}
	}

	public Display get()
	{
		if ( imageDisplay != null ) return imageDisplay;
		else if ( segmentationDisplay != null ) return segmentationDisplay;
		else throw new RuntimeException( "No SourceDisplay set" );
	}
}
