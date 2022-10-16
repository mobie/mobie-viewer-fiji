package org.embl.mobie.viewer.image;

import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.type.numeric.IntegerType;
import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.annotation.AnnotationAdapter;
import org.embl.mobie.viewer.source.AnnotatedLabelSource;
import org.embl.mobie.viewer.source.AnnotationType;
import org.embl.mobie.viewer.source.SourcePair;
import org.embl.mobie.viewer.source.VolatileAnnotatedLabelSource;
import org.embl.mobie.viewer.table.AnnData;

public class DefaultAnnotatedLabelImage< A extends Annotation > implements AnnotatedLabelImage< A >
{
	protected Image< ? extends IntegerType< ? > > labelImage;
	protected AnnData< A > annData;
	protected SourcePair< AnnotationType< A > > sourcePair;
	private RealMaskRealInterval mask;
	private AnnotationAdapter<A> annotationAdapter;

	public DefaultAnnotatedLabelImage( Image< ? extends IntegerType< ? > > labelImage, AnnData< A > annData, AnnotationAdapter< A > annotationAdapter )
	{
		this.labelImage = labelImage;
		this.annData = annData;
		this.annotationAdapter = annotationAdapter;
	}

	@Override
	public SourcePair< AnnotationType< A > > getSourcePair()
	{
		if ( sourcePair == null )
		{
			final SourcePair< ? extends IntegerType< ? > > sourcePair = labelImage.getSourcePair();

			// non-volatile source
			final AnnotatedLabelSource< ?, A > source = new AnnotatedLabelSource( sourcePair.getSource(), annotationAdapter );

			if ( sourcePair.getVolatileSource() == null )
			{
				this.sourcePair = new DefaultSourcePair<>( source, null );
			}
			else
			{
				// volatile source
				final VolatileAnnotatedLabelSource< ?, ? extends Volatile< ? >, A > volatileSource = new VolatileAnnotatedLabelSource( getLabelImage().getSourcePair().getVolatileSource(), annotationAdapter );
				this.sourcePair = new DefaultSourcePair<>( source, volatileSource );
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
	public void transform( AffineTransform3D affineTransform3D )
	{
		// TODO: it is not so clear to me whether to actually transform the
		//   labelImage or only the annotatedLabelImage (in the open() function).
		labelImage.transform( affineTransform3D );
		annData.getTable().transform( affineTransform3D );
	}

	@Override
	public RealMaskRealInterval getMask()
	{
		return labelImage.getMask();
	}

	@Override
	public void setMask( RealMaskRealInterval mask )
	{
		labelImage.setMask( mask );
	}

	@Override
	public AnnData< A > getAnnData()
	{
		return annData;
	}

	@Override
	public Image< ? extends IntegerType< ? > > getLabelImage()
	{
		return labelImage;
	}
}
