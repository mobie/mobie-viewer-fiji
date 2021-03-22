package de.embl.cba.mobie2;

public class SourceSupplier // TODO: make it a supplier
{
	public ImageSource image;
	public SegmentationSource segmentation;

	public MoBIESource getSource()
	{
		if ( image != null ) return image;
		else if ( segmentation != null ) return segmentation;
		else throw new RuntimeException( "Unsupported Source." );
	}
}
