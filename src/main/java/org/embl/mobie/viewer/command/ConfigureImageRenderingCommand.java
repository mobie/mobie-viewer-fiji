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
package org.embl.mobie.viewer.command;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import org.embl.mobie.viewer.bdv.render.BlendingMode;
import org.embl.mobie.viewer.color.CategoricalAnnotationColoringModel;
import org.embl.mobie.viewer.color.ColoringModel;
import org.embl.mobie.viewer.color.MobieColoringModel;
import org.embl.mobie.viewer.color.MobieColoringModelWrapper;
import org.embl.mobie.viewer.source.BoundarySource;
import org.embl.mobie.viewer.source.SourceHelper;
import org.embl.mobie.viewer.source.VolatileBoundarySource;
import org.scijava.Initializable;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.ColorRGB;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.services.ISourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.Arrays;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Display>Configure Image Rendering")
public class ConfigureImageRenderingCommand extends DynamicCommand implements BdvPlaygroundActionCommand, Initializable
{
	protected static ISourceAndConverterService sourceAndConverterService = SourceAndConverterServices.getSourceAndConverterService();

	@Parameter
	protected BdvHandle bdvh;

	@Parameter
	protected SourceAndConverter< ? >[] sourceAndConverters;

	@Parameter( label = "Blending Mode", choices = { BlendingMode.SUM, BlendingMode.ALPHA }, persist = false )
	String blendingMode = BlendingMode.SUM;

	@Override
	public void initialize()
	{
		initBlendingModeItem();
	}

	private void initBlendingModeItem()
	{
		final MutableModuleItem< String > blendingModeItem = getInfo().getMutableInput("blendingMode", String.class );

		for ( SourceAndConverter sourceAndConverter : sourceAndConverters )
		{
			final BlendingMode blendingMode = ( BlendingMode ) sourceAndConverterService.getMetadata( sourceAndConverter, BlendingMode.class.getName() );
			final String toString = blendingMode.toString();
			blendingModeItem.setValue( this, toString );
			return;
		}
	}

	@Override
	public void run()
	{
		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			final BlendingMode blendingMode = BlendingMode.valueOf( this.blendingMode );
			SourceAndConverterServices.getSourceAndConverterService().setMetadata( sourceAndConverter, BlendingMode.class.getName(), blendingMode );
		}

		bdvh.getViewerPanel().requestRepaint();
	}
}
