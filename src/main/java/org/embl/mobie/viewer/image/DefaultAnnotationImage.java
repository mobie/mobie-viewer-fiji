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

public class DefaultAnnotationImage< A extends Annotation > implements AnnotationImage< A >
{
	private final String name;
	protected Image< AnnotationType< A > > annotationTypeImage;
	protected AnnData< A > annData;
	protected SourcePair< AnnotationType< A > > sourcePair;
	private RealMaskRealInterval mask;

	public DefaultAnnotationImage( String name, Image< AnnotationType< A > > annotationTypeImage, AnnData< A > annData )
	{
		this.name = name;
		this.annotationTypeImage = annotationTypeImage;
		this.annData = annData;
	}

	@Override
	public SourcePair< AnnotationType< A > > getSourcePair()
	{
		return annotationTypeImage.getSourcePair();
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public void transform( AffineTransform3D affineTransform3D )
	{
		annotationTypeImage.transform( affineTransform3D );
		annData.getTable().transform( affineTransform3D );
	}

	@Override
	public RealMaskRealInterval getMask()
	{
		return annotationTypeImage.getMask();
	}

	@Override
	public void setMask( RealMaskRealInterval mask )
	{
		annotationTypeImage.setMask( mask );
	}

	@Override
	public AnnData< A > getAnnData()
	{
		return annData;
	}
}
