package org.embl.mobie.viewer.source;

import org.embl.mobie.viewer.annotation.AnnotationProvider;
import org.embl.mobie.viewer.annotation.AnnotationProviderInterface;
import org.embl.mobie.viewer.table.AnnData;
import org.embl.mobie.viewer.annotation.AnnotatedSegment;
import net.imglib2.Volatile;
import net.imglib2.type.numeric.IntegerType;

// TODO: Maybe I can use the same for the region image?!
public class SegmentationImage< AS extends AnnotatedSegment > implements AnnotatedImage< AS >
{
	protected Image< ? extends IntegerType< ? > > labelMask;
	protected AnnData< AS > annData;
	protected SourcePair< AnnotationType< AS > > sourcePair;

	public SegmentationImage()
	{
	}

	public SegmentationImage( Image< ? extends IntegerType< ? > > labelMask, AnnData< AS > annData )
	{
		this.labelMask = labelMask;
		this.annData = annData;
	}

	@Override
	public SourcePair< AnnotationType< AS > > getSourcePair()
	{
		if ( sourcePair == null )
		{
			AnnotationProviderInterface< AS > annotationProvider = new AnnotationProvider( annData );
			final AnnotatedLabelMaskSource< ?, AS > source = new AnnotatedLabelMaskSource( getLabelMask().getSourcePair().getSource(), annotationProvider );
			final VolatileAnnotatedIntegerTypeSource< ?, ? extends Volatile< ? >, AS > volatileSource = new VolatileAnnotatedIntegerTypeSource( getLabelMask().getSourcePair().getVolatileSource(), annotationProvider );
			sourcePair = new DefaultSourcePair<>( source, volatileSource );
		}

		return sourcePair;
	}

	@Override
	public String getName()
	{
		return labelMask.getName();
	}

	public Image< ? extends IntegerType< ? > > getLabelMask()
	{
		return labelMask;
	}

	@Override
	public AnnData< AS > getAnnData()
	{
		return annData;
	}
}
