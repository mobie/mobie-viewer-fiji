/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
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

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.source.AnnotationType;
import org.embl.mobie.lib.table.AnnData;

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
