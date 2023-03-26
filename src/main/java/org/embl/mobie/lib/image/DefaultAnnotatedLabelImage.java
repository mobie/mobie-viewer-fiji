package org.embl.mobie.lib.image;

import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.type.numeric.IntegerType;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.annotation.AnnotationAdapter;
import org.embl.mobie.lib.source.AnnotatedLabelSource;
import org.embl.mobie.lib.source.AnnotationType;
import org.embl.mobie.lib.source.VolatileAnnotatedLabelSource;
import org.embl.mobie.lib.table.AnnData;

public class DefaultAnnotatedLabelImage< A extends Annotation > implements AnnotatedLabelImage< A >
{
	protected Image< ? extends IntegerType< ? > > labelImage;
	protected AnnData< A > annData;
	protected SourcePair< AnnotationType< A > > sourcePair;
	private AnnotationAdapter< A > annotationAdapter;

	// TODO This should probably also expose the annotationAdapter in a getter
	//   already in the interface
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
		for ( ImageListener listener : listeners.list )
			listener.imageChanged();
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
