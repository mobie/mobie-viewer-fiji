package de.embl.cba.mobie2.source;

import de.embl.cba.mobie2.source.ImageSource;
import de.embl.cba.mobie2.source.SegmentationSource;

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
