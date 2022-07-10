package mobie3.viewer.source;

import mobie3.viewer.segment.LabelToSegmentMapper;
import mobie3.viewer.segment.SegmentProvider;
import mobie3.viewer.table.AnnData;
import mobie3.viewer.table.SegmentRow;
import mobie3.viewer.transform.ImageTransformer;
import net.imglib2.Volatile;
import net.imglib2.type.numeric.IntegerType;

public class TransformedAnnotatedLabelMask< T extends IntegerType< T >, SR extends SegmentRow > extends AnnotatedLabelMask< T, SR >
{
	private final AnnotatedImage< T, SR > annotatedImage;
	private final ImageTransformer transformer;

	public TransformedAnnotatedLabelMask( AnnotatedImage< T, SR > annotatedImage, ImageTransformer transformer )
	{
		super();
		this.annotatedImage = annotatedImage;
		this.transformer = transformer;
		this.annData = annotatedImage.getAnnData(); // transform?
	}

	@Override
	public Image< T > getLabelMask()
	{
		if ( labelMask == null )
			labelMask = new TransformedImage<>( annotatedImage.getLabelMask(), transformer );

		return labelMask;
	}

	@Override
	public AnnData< SR > getAnnData()
	{
		return annData;
	}


	@Override
	public String getName()
	{
		// get name from image transformer
		return null;
	}
}
