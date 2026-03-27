/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.lib.image;

import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.annotation.AnnotationAdapter;
import org.embl.mobie.lib.source.AnnotationType;
import org.embl.mobie.lib.source.label.AnnotatedLabelSource;
import org.embl.mobie.lib.source.label.NumericAnnotationSource;
import org.embl.mobie.lib.source.label.VolatileAnnotatedLabelSource;
import org.embl.mobie.lib.table.AnnData;

public class NumericAnnotationImage< R extends RealType< R >, A extends Annotation > implements Image< R >, ImageWrapper
{
    private final Image< AnnotationType< A > > annotationTypeImage;
    private final String featureName;
    private final String name;
	private SourcePair< R > sourcePair;

	public NumericAnnotationImage( Image< AnnotationType< A > > annotationImage, String featureName )
	{
        this.annotationTypeImage = annotationImage;
        this.featureName = featureName;
        this.name = annotationImage.getName() + "_" + featureName;
    }

	@Override
	public SourcePair< R > getSourcePair()
	{
		if ( sourcePair == null )
		{
			SourcePair< AnnotationType< A > > annotationTypeSourcePair = annotationTypeImage.getSourcePair();

			// non-volatile source
			final AnnotatedLabelSource< ?, A > source = new NumericAnnotationSource( annotationTypeSourcePair.getSource(), featureName );

			annotationAdapter.init();

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
		return name;
	}

	@Override
	public void transform( AffineTransform3D affineTransform3D )
	{
		annotationTypeImage.transform( affineTransform3D );
		for ( ImageListener listener : listeners.list )
			listener.imageChanged();
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
	public Image< ? > getWrappedImage()
	{
		return annotationTypeImage;
	}
}
