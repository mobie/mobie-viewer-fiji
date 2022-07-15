package mobie3.viewer.source;

import mobie3.viewer.annotation.LabelToSegmentMapper;
import mobie3.viewer.annotation.SegmentProvider;
import mobie3.viewer.table.AnnData;
import mobie3.viewer.annotation.AnnotatedSegment;
import mobie3.viewer.table.SegmentsAnnData;
import net.imglib2.Volatile;
import net.imglib2.type.numeric.IntegerType;

public class AnnotatedLabelMask< T extends IntegerType< T >, AS extends AnnotatedSegment > implements AnnotatedImage< AS >
{
	protected Image< T > labelMask;
	protected SegmentsAnnData< AS > annData;
	protected SourcePair< AnnotationType< AS > > sourcePair;

	public AnnotatedLabelMask()
	{
	}

	public AnnotatedLabelMask( Image< T > labelMask, SegmentsAnnData< AS > annData )
	{
		this.labelMask = labelMask;
		this.annData = annData;
	}

	@Override
	public SourcePair< AnnotationType< AS > > getSourcePair()
	{
		if ( sourcePair == null )
		{
			SegmentProvider< AS > segmentProvider = new LabelToSegmentMapper( annData );
			final AnnotatedLabelMaskSource< T, AS > source = new AnnotatedLabelMaskSource<>( getLabelMask().getSourcePair().getSource(), segmentProvider );
			final VolatileAnnotatedLabelMaskSource< T, ? extends Volatile< T >, AS > volatileSource = new VolatileAnnotatedLabelMaskSource<>( getLabelMask().getSourcePair().getVolatileSource(), segmentProvider );
			sourcePair = new DefaultSourcePair<>( source, volatileSource );
		}

		return sourcePair;
	}

	@Override
	public String getName()
	{
		return labelMask.getName();
	}

	@Override
	public Image< T > getLabelMask()
	{
		return labelMask;
	}

	@Override
	public AnnData< AS > getAnnData()
	{
		return null;
	}
}
