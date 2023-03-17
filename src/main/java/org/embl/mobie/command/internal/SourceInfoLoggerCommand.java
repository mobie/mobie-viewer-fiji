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
package org.embl.mobie.command.internal;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.command.CommandConstants;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Plugin( type = BdvPlaygroundActionCommand.class, name = SourceInfoLoggerCommand.NAME, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + SourceInfoLoggerCommand.NAME )
public class SourceInfoLoggerCommand implements BdvPlaygroundActionCommand
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public static final String NAME = "Log Source(s) Info";

	@Parameter
	BdvHandle bdvHandle;

	@Override
	public void run()
	{
		new Thread( () -> {
			final int t = bdvHandle.getViewerPanel().state().getCurrentTimepoint();

			final Set< SourceAndConverter< ? > > sourceAndConverters = bdvHandle.getViewerPanel().state().getVisibleAndPresentSources();

			final List< ? extends Source< ? > > sources = sourceAndConverters.stream().map( sac -> sac.getSpimSource() ).sorted( Comparator.comparing(  s -> s.getName() ) ).collect( Collectors.toList() );

			for ( Source< ? > source : sources )
			{
				IJ.log( "\n## " + source.getName()  );
				final AffineTransform3D affineTransform3D = new AffineTransform3D();
				source.getSourceTransform( t, 0, affineTransform3D );
				final VoxelDimensions voxelDimensions = source.getVoxelDimensions();
				if ( voxelDimensions.numDimensions() > 0 )
					IJ.log( voxelDimensions.toString() );
				IJ.log( "Resolution levels: " + source.getNumMipmapLevels() );
				IJ.log( "Data type: " + source.getType().getClass() );
				IJ.log( "Shape: " + Arrays.toString( source.getSource( 0,0 ).dimensionsAsLongArray() ) );
				IJ.log( "Transform from array to global space: " + affineTransform3D );

			}
		} ).start();
	}
}
