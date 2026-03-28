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
package org.embl.mobie.lib.source;

import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.realtransform.AffineTransform3D;

// During the wrapping the type changes from A to B
public abstract class AbstractSourceWrapper< A, B > implements Source< B >, SourceWrapper< A >
{
    protected final Source< A > wrappedSource;
    private final String name;

    public AbstractSourceWrapper( final Source< A > wrappedSource )
    {
        this.wrappedSource = wrappedSource;
        this.name = wrappedSource.getName();
    }

    public AbstractSourceWrapper( Source< A > wrappedSource, String name  )
    {
        this.wrappedSource = wrappedSource;
        this.name = name;
    }

    @Override
    public boolean doBoundingBoxCulling() {
        return wrappedSource.doBoundingBoxCulling();
    }

    @Override
    public synchronized void getSourceTransform(final int t, final int level, final AffineTransform3D transform) {
        wrappedSource.getSourceTransform(t, level, transform);
    }

    @Override
    public boolean isPresent( final int t )
    {
        return wrappedSource.isPresent(t);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public VoxelDimensions getVoxelDimensions() {
        return wrappedSource.getVoxelDimensions();
    }

    @Override
    public int getNumMipmapLevels() {
        return wrappedSource.getNumMipmapLevels();
    }

    @Override
    public Source< A > getWrappedSource() {
        return wrappedSource;
    }
}
