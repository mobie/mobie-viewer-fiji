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

import bdv.viewer.SourceAndConverter;
import org.embl.mobie.lib.image.SpotAnnotationImage;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Display>Configure Spot Rendering")
public class ConfigureSpotRenderingCommand extends ConfigureLabelRenderingCommand
{
	@Parameter( label = "Spot radius", style = "format:#.00", persist = false )
	public Double spotRadius = 1.0;

	@Override
	public void initialize()
	{
		super.initialize();
		initSpotRadiusItem();
	}

	@Override
	public void run()
	{
		configureBoundaryRendering();

		configureSelectionColoring();

		configureRandomColorSeed();

		configureSpotRadius();

		bdvh.getViewerPanel().requestRepaint();
	}

	private void initSpotRadiusItem()
	{
		final MutableModuleItem< Double > spotRadiusItem = getInfo().getMutableInput("spotRadius", Double.class );

		for ( SourceAndConverter sourceAndConverter : sourceAndConverters )
		{
			final SpotAnnotationImage spotAnnotationImage = ( SpotAnnotationImage ) sourceAndConverterService.getMetadata( sourceAndConverter, SpotAnnotationImage.class.getName() );
			spotRadiusItem.setValue( this, spotAnnotationImage.getRadius() );
			return;
		}
	}

	private void configureSpotRadius()
	{
		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			final SpotAnnotationImage spotAnnotationImage = ( SpotAnnotationImage ) sourceAndConverterService.getMetadata( sourceAndConverter, SpotAnnotationImage.class.getName() );
			spotAnnotationImage.setRadius( spotRadius );
		}
	}

}
