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
import net.imglib2.type.numeric.IntegerType;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.annotation.AnnotationAdapter;
import org.embl.mobie.lib.source.AnnotatedLabelSource;
import org.embl.mobie.lib.source.AnnotationType;
import org.embl.mobie.lib.source.VolatileAnnotatedLabelSource;
import org.embl.mobie.lib.table.AnnData;

public class DefaultAnnotationLabelImage< A extends Annotation > implements AnnotationLabelImage< A >
{
	protected Image< ? extends IntegerType< ? > > labelImage;
	protected AnnData< A > annData;
	protected SourcePair< AnnotationType< A > > sourcePair;
	private AnnotationAdapter< A > annotationAdapter;

	public DefaultAnnotationLabelImage( Image< ? extends IntegerType< ? > > labelImage,
										AnnData< A > annData,
										AnnotationAdapter< A > annotationAdapter )
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
		return labelImage.getName();
	}

	@Override
	public void transform( AffineTransform3D affineTransform3D )
	{
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

	@Override
	public AnnotationAdapter< A > getAnnotationAdapter()
	{
		return annotationAdapter;
	}

	@Override
	public Image< ? > getWrappedImage()
	{
		return labelImage;
	}
}
