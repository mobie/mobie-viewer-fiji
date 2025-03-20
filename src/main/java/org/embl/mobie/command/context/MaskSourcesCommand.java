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
package org.embl.mobie.command.context;

import bdv.util.BdvHandle;
import net.imglib2.roi.RealMaskRealInterval;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.command.widget.SelectableImages;
import org.embl.mobie.lib.data.DataStore;
import org.embl.mobie.lib.image.CroppedImage;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.image.MaskedImage;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Mask Image")
public class MaskSourcesCommand extends BoxSelectionCommand
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    @Parameter ( label = "Mask Image(s)" )
    public SelectableImages selectedImages;

    @Override
    public void run()
    {
        super.run();
        RealMaskRealInterval mask = transformedBox.asMask();
        List< ? extends Image< ? > > images = selectedImages.getNames().stream()
                .map( name -> DataStore.getImage( name ) )
                .collect( Collectors.toList() );
        for ( Image< ? > image : images )
        {
            new MaskedImage<>( image, image.getName() + "_masked", mask );
        }
    }

}
