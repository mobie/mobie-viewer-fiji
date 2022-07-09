package mobie3.viewer.segment;

import de.embl.cba.tables.imagesegment.ImageSegment;

import java.util.Objects;

public class Label
{
	private final String image;
	private final double label;
	private final int frame;

	public Label( double label, int frame, String image )
	{
		this.label = label;
		this.frame = frame;
		this.image = image;

	}

	public Label( Segment segment )
	{
		this.image = segment.imageId();
		this.label = segment.labelId();
		this.frame = segment.timePoint();
	}

	@Override
	public boolean equals( Object o )
	{
		if ( this == o ) return true;
		if ( o == null || getClass() != o.getClass() ) return false;
		Label that = ( Label ) o;
		return Double.compare( that.label, label ) == 0 &&
				frame == that.frame &&
				Objects.equals( image, that.image );
	}

	@Override
	public int hashCode()
	{
		return Objects.hash( label, image, frame );
	}

	public String getImage()
	{
		return image;
	}

	public double getLabel()
	{
		return label;
	}

	public int getFrame()
	{
		return frame;
	}
}