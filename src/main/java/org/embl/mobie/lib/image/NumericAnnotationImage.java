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

import bdv.viewer.Source;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.type.numeric.real.DoubleType;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.source.AnnotationType;
import org.embl.mobie.lib.source.label.NumericAnnotationSource;
import org.embl.mobie.lib.source.label.VolatileNumericAnnotationSource;

public class NumericAnnotationImage< A extends Annotation > implements Image< DoubleType >, ImageWrapper
{
    private final Image< AnnotationType< A > > annotationImage;
    private final String featureName;
    private final String name;
	private SourcePair< DoubleType > sourcePair;

	public NumericAnnotationImage( Image< AnnotationType< A > > annotationImage, String featureName )
	{
        this.annotationImage = annotationImage;
        this.featureName = featureName;
        this.name = annotationImage.getName() + "_" + featureName;
    }

	@Override
	public SourcePair< DoubleType > getSourcePair()
	{
		if ( sourcePair == null )
		{
			SourcePair< AnnotationType< A > > annotationSourcePair = annotationImage.getSourcePair();

			// non-volatile source
			Source< DoubleType > numericAnnotationSource = new NumericAnnotationSource<>( annotationSourcePair.getSource(), featureName );

			if ( annotationSourcePair.getVolatileSource() == null )
			{
				sourcePair = new DefaultSourcePair< >( numericAnnotationSource, null );
			}
			else
			{
				// volatile source
				VolatileNumericAnnotationSource< A, ? extends Volatile< AnnotationType< A > > > volatileNumericAnnotationSource = new VolatileNumericAnnotationSource<>( annotationImage.getSourcePair().getVolatileSource(), featureName );
				this.sourcePair = new DefaultSourcePair<>( numericAnnotationSource, volatileNumericAnnotationSource );
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
		annotationImage.transform( affineTransform3D );
		for ( ImageListener listener : listeners.list )
			listener.imageChanged();
	}

	@Override
	public RealMaskRealInterval getMask()
	{
		return annotationImage.getMask();
	}

	@Override
	public void setMask( RealMaskRealInterval mask )
	{
		annotationImage.setMask( mask );
	}

	@Override
	public Image< ? > getWrappedImage()
	{
		return annotationImage;
	}
}
