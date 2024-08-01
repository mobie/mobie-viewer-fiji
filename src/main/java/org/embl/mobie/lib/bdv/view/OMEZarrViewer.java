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
package org.embl.mobie.lib.bdv.view;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

import java.util.List;

public class OMEZarrViewer
{
    private final List< SourceAndConverter< ? > > sacs;
    private BdvHandle bdvHandle;
    private SourceAndConverterService sacService;
    private SourceAndConverterBdvDisplayService sacDisplayService;

    public OMEZarrViewer( List< SourceAndConverter< ? > > sacs )
    {
        this.sacs = sacs;
    }

    public void show()
    {
        for ( SourceAndConverter< ? > sac : sacs )
        {
            if ( bdvHandle == null  )
                bdvHandle = BdvFunctions.show( sac ).getBdvHandle();
            else
                BdvFunctions.show( sac, new BdvOptions().addTo( bdvHandle ) );
        }

    }

    // TODO: consider using the below code instead.

//    private void showSacs( Collection< SourceAndConverter< ? > > sourceAndConverters )
//    {
//        for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
//        {
//            sacDisplayService.show( bdvHandle, sourceAndConverter );
//        }
//    }
//
//    private void installContextMenu( BdvHandle bdvHandle )
//    {
//        final ArrayList< String > actions = new ArrayList< String >();
//        actions.add( sacService.getCommandName( ScreenShotMakerCommand.class ) );
//        actions.add( sacService.getCommandName( ShowRasterImagesCommand.class ) );
//
//        SourceAndConverterContextMenuClickBehaviour contextMenu = new SourceAndConverterContextMenuClickBehaviour( bdvHandle, new SourcesAtMousePositionSupplier( bdvHandle, false ), actions.toArray( new String[ 0 ] ) );
//
//        Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
//        behaviours.behaviour( contextMenu, "Context menu", "button3");
//        behaviours.install( bdvHandle.getTriggerbindings(), "MoBIE" );
//    }
//
//    public BdvOptions getBdvOptions(List<ViewSetup> viewSetups) {
//        boolean is2D = is2D(viewSetups);
//        BdvOptions bdvOptions = new BdvOptions();
//
//        if (is2D) {
//            bdvOptions = bdvOptions.is2D();
//        }
//        return bdvOptions;
//    }
//
//    public boolean is2D(List<ViewSetup> viewSetups) {
//        for (ViewSetup viewSetup : viewSetups) {
//            final Dimensions size = viewSetup.getSize();
//            if (size.dimensionsAsLongArray().length > 2) {
//                if (size.dimension(2) > 1)
//                    return false;
//            }
//        }
//        return true;
//    }
}
