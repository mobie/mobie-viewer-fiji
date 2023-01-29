/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
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
package org.embl.mobie.lib.serialize.display;

import bdv.viewer.SourceAndConverter;
import org.embl.mobie.lib.bdv.render.BlendingMode;
import org.embl.mobie.lib.image.Image;

import java.util.List;


/**
 * A display is a collection of images
 * with shared display settings.
 *
 * Currently, everything that we display in MoBIE
 * is a display. This implies that it must be
 * modelled as an Image, which essentially is a Source.
 * This is in line with BDV, which internally also
 * models everything as a Source.
 *
 * Practically, for adding a Display to BDV,
 * all images, using the display settings, will be
 * converted to SourceAndConverters.
 *
 * @param <T> the data type of the images
 */
public interface Display< T >
{
	String getName();
	List< String > getSources();
	BlendingMode getBlendingMode();
	double getOpacity();
	boolean isVisible();
	List< Image< T > > images();
	List< SourceAndConverter< T > > sourceAndConverters();
}
