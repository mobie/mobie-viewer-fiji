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
package org.embl.mobie.command.context;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.MoBIE;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.lib.MoBIEHelper;
import org.embl.mobie.lib.bdv.ScreenShotMaker;
import org.scijava.Initializable;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Log Images Info")
public class SourcesInfoCommand implements BdvPlaygroundActionCommand
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    public static final String CAPTURE_SIZE_PIXELS = "Capture size [pixels]: ";

    @Parameter
    public BdvHandle bdvHandle;

    @Override
    public void run()
    {
        List< SourceAndConverter< ? > > visibleSacs = MoBIEHelper.getVisibleSacs( bdvHandle );
        visibleSacs.forEach( sac ->
        {
            Source< ? > source = sac.getSpimSource();
            IJ.log( "# " + source.getName() );
            IJ.log( "Number of resolution levels: " + source.getNumMipmapLevels() );
            IJ.log( "Voxel size: " + Arrays.toString( source.getVoxelDimensions().dimensionsAsDoubleArray() ) );

            if ( source instanceof TransformedSource )
            {
                AffineTransform3D transform3D = new AffineTransform3D();
                ( ( TransformedSource<?> ) source ).getFixedTransform( transform3D );
                IJ.log( "Additional transform: " + transform3D );
                source.getSourceTransform( 0, 0, transform3D );
                IJ.log( "Full transform: " + transform3D );
            }
        });
    }
}
