package org.embl.mobie.viewer.source;

import net.imglib2.roi.RealMaskRealInterval;
import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.annotation.AnnotationAdapter;
import org.embl.mobie.viewer.table.AnnData;
import net.imglib2.Volatile;
import net.imglib2.type.numeric.IntegerType;

public class AnnotatedLabelImage< A extends Annotation > implements AnnotatedImage< A >
{
	protected Image< ? extends IntegerType< ? > > labelImage;
	protected AnnData< A > annData;
	protected SourcePair< AnnotationType< A > > sourcePair;

	public AnnotatedLabelImage( Image< ? extends IntegerType< ? > > labelImage, AnnData< A > annData )
	{
		this.labelImage = labelImage;
		this.annData = annData;
	}

	@Override
	public SourcePair< AnnotationType< A > > getSourcePair()
	{
		if ( sourcePair == null )
		{
			AnnotationAdapter< A > annotationAdapter = new AnnotationAdapter( annData );
			final AnnotatedLabelSource< ?, A > source = new AnnotatedLabelSource( getLabelImage().getSourcePair().getSource(), annotationAdapter );
			if (  getLabelImage().getSourcePair().getVolatileSource() != null )
			{
				final VolatileAnnotatedLabelSource< ?, ? extends Volatile< ? >, A > volatileSource = new VolatileAnnotatedLabelSource( getLabelImage().getSourcePair().getVolatileSource(), annotationAdapter );
				sourcePair = new DefaultSourcePair<>( source, volatileSource );
			}
			else
			{
				sourcePair = new DefaultSourcePair<>( source, null );
			}
		}

		return sourcePair;
	}

	@Override
	public String getName()
	{
		return labelImage.getName();
	}

	@Override
	public RealMaskRealInterval getMask()
	{
		return labelImage.getMask();
	}

	public Image< ? extends IntegerType< ? > > getLabelImage()
	{
		return labelImage;
	}

	@Override
	public AnnData< A > getAnnData()
	{
		return annData;
	}
}
