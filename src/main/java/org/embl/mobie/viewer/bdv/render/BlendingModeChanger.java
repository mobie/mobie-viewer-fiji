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
package org.embl.mobie.viewer.bdv.render;

import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import java.util.function.Consumer;


public class BlendingModeChanger implements Runnable, Consumer< SourceAndConverter[] > {

    private BlendingMode blendingMode;
    private final SourceAndConverter[] sacs;

    public BlendingModeChanger( SourceAndConverter[] sacs, BlendingMode blendingMode ) {
        this.sacs = sacs;
        this.blendingMode = blendingMode;
    }

    @Override
    public void run() {
        accept( sacs );
    }

    @Override
    public void accept(SourceAndConverter[] sourceAndConverter) {

        changeProjectionMode();
        updateDisplays();
    }

    private void updateDisplays()
    {
        if ( SourceAndConverterServices.getBdvDisplayService()!=null)
            SourceAndConverterServices.getBdvDisplayService().updateDisplays( sacs );

    }

    private void changeProjectionMode()
    {
        for ( SourceAndConverter sac : sacs )
        {
            SourceAndConverterServices.getSourceAndConverterService().setMetadata( sac, BlendingMode.BLENDING_MODE, blendingMode );
        }
    }
}
