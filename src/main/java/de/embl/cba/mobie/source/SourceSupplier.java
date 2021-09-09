package de.embl.cba.mobie.source;

// TODO: get rid of the supplier and deal with this by means of a Gson adapter
public class SourceSupplier
{
	private ImageSource image;
	private SegmentationSource segmentation;

	public SourceSupplier( ImageSource imageSource ) {
		this.image = imageSource;
	}

	public SourceSupplier( SegmentationSource segmentationSource ) {
		this.segmentation = segmentationSource;
	}

	public ImageSource get()
	{
		if ( image != null ) return image;
		else if ( segmentation != null ) return segmentation;
		else throw new RuntimeException( "Unsupported Source." );
	}
}
