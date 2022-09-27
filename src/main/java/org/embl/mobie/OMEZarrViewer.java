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
package org.embl.mobie;

import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.Prefs;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.sequence.ViewSetup;
import net.imglib2.Dimensions;
import net.imglib2.type.numeric.ARGBType;
import org.embl.mobie.viewer.bdv.SourcesAtMousePositionSupplier;
import org.embl.mobie.viewer.bdv.view.SliceViewer;
import org.embl.mobie.viewer.command.ScreenShotMakerCommand;
import org.embl.mobie.viewer.command.ShowRasterImagesCommand;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.PlaygroundPrefs;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.behaviour.SourceAndConverterContextMenuClickBehaviour;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.importer.SourceAndConverterFromSpimDataCreator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OMEZarrViewer
{
    private final SpimData spimData;
    private BdvHandle bdvHandle;
    private SourceAndConverterService sacService;
    private SourceAndConverterBdvDisplayService sacDisplayService;

    public OMEZarrViewer( SpimData spimData )
    {
        this.spimData = spimData;
    }

    public void show() {

        final SourceAndConverterFromSpimDataCreator creator = new SourceAndConverterFromSpimDataCreator( spimData );

        final Collection< SourceAndConverter< ? > > sourceAndConverters = creator.getSetupIdToSourceAndConverter().values();

        // TODO: numTimePoints
        // TODO: is2D
        // SourceAndConverterHelper.getMaxTimepoint(  )

        Prefs.showScaleBar( true );
        Prefs.showMultibox( true );
        Prefs.scaleBarColor( ARGBType.rgba( 255, 255, 255, 255 ) );
        PlaygroundPrefs.setSourceAndConverterUIVisibility( false );
        bdvHandle = SliceViewer.createBdv( false, "OME-Zarr - BigDataViewer" );
        sacService = ( SourceAndConverterService ) SourceAndConverterServices.getSourceAndConverterService();
        sacDisplayService = SourceAndConverterServices.getBdvDisplayService();

        showSacs( sourceAndConverters );
        new ViewerTransformAdjuster( bdvHandle, sourceAndConverters.iterator().next() ).run();
        installContextMenu( bdvHandle );
    }

    private void showSacs( Collection< SourceAndConverter< ? > > sourceAndConverters )
    {
        for ( SourceAndConverter sourceAndConverter : sourceAndConverters )
        {
            sacDisplayService.show( bdvHandle, sourceAndConverter );
        }

//        for (int i = 0; i < viewSetups.size(); i++) {
//            final String name = viewSetups.get(i).getChannel().getName();
//            if ( name.contains("labels") )
//                Sources.showAsLabelMask( sources.get(i) );
//        }
    }

    private void installContextMenu( BdvHandle bdvHandle )
    {
        final ArrayList< String > actions = new ArrayList< String >();
        actions.add( sacService.getCommandName( ScreenShotMakerCommand.class ) );
        actions.add( sacService.getCommandName( ShowRasterImagesCommand.class ) );

        SourceAndConverterContextMenuClickBehaviour contextMenu = new SourceAndConverterContextMenuClickBehaviour( bdvHandle, new SourcesAtMousePositionSupplier( bdvHandle, false ), actions.toArray( new String[ 0 ] ) );

        Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
        behaviours.behaviour( contextMenu, "Context menu", "button3");
        behaviours.install( bdvHandle.getTriggerbindings(), "MoBIE" );
    }

    public BdvOptions getBdvOptions(List<ViewSetup> viewSetups) {
        boolean is2D = is2D(viewSetups);
        BdvOptions bdvOptions = new BdvOptions();

        if (is2D) {
            bdvOptions = bdvOptions.is2D();
        }
        return bdvOptions;
    }

    public boolean is2D(List<ViewSetup> viewSetups) {
        for (ViewSetup viewSetup : viewSetups) {
            final Dimensions size = viewSetup.getSize();
            if (size.dimensionsAsLongArray().length > 2) {
                if (size.dimension(2) > 1)
                    return false;
            }
        }
        return true;
    }
}
