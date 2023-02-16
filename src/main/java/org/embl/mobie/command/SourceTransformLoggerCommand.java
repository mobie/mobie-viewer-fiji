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
package org.embl.mobie.command;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import com.google.gson.Gson;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.Logger;
import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.lib.serialize.JsonHelper;
import org.embl.mobie.lib.transform.AffineViewerTransform;
import org.embl.mobie.lib.transform.NormalVectorViewerTransform;
import org.embl.mobie.lib.transform.NormalizedAffineViewerTransform;
import org.embl.mobie.lib.transform.TransformHelper;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.Set;

@Plugin( type = BdvPlaygroundActionCommand.class, name = SourceTransformLoggerCommand.NAME, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + SourceTransformLoggerCommand.NAME )
public class SourceTransformLoggerCommand implements BdvPlaygroundActionCommand
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public static final String NAME = "Log Source Transforms";

	@Parameter
	BdvHandle bdvHandle;

	@Override
	public void run()
	{
		new Thread( () -> {
			final int t = bdvHandle.getViewerPanel().state().getCurrentTimepoint();

			final Set< SourceAndConverter< ? > > sourceAndConverters = bdvHandle.getViewerPanel().state().getVisibleAndPresentSources();

			for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
			{
				final AffineTransform3D affineTransform3D = new AffineTransform3D();
				final Source< ? > source = sourceAndConverter.getSpimSource();
				source.getSourceTransform( t, 0, affineTransform3D );
				IJ.log(  source.getName() + ": " + affineTransform3D );
			}
		} ).start();
	}
}
