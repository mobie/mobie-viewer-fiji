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
package mobie3.viewer.command;

import bdv.viewer.SourceAndConverter;
import mobie3.viewer.volume.SegmentsVolumeViewer;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import static org.scijava.ItemVisibility.MESSAGE;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Display>Configure Label Volume Rendering")
public class ConfigureLabelVolumeRenderingCommand implements BdvPlaygroundActionCommand
{
	@Parameter
	public SourceAndConverter[] sourceAndConverters;

	@Parameter ( label = "Resolution X", style="format:#.000" )
	public double sx;

	@Parameter ( label = "Resolution Y", style="format:#.000" )
	public double sy;

	@Parameter ( label = "Resolution Z", style="format:#.000" )
	public double sz;

	@Parameter ( visibility = MESSAGE )
	String msg = "( Resolution units: see BigDataViewer scale bar )";

	@Parameter ( label = "Repaint segments")
	public boolean repaint;

	@Override
	public void run()
	{
		setVoxelSpacing( sourceAndConverters, new double[]{ sx, sy, sz }, repaint );
	}

	private void setVoxelSpacing( SourceAndConverter[] sourceAndConverters, double[] voxelSpacing, boolean repaint )
	{
		final SourceAndConverterService sacService = ( SourceAndConverterService ) SourceAndConverterServices.getSourceAndConverterService();

		for ( SourceAndConverter sourceAndConverter : sourceAndConverters )
		{
			final SegmentsVolumeViewer volumeViewer = ( SegmentsVolumeViewer ) sacService.getMetadata( sourceAndConverter, SegmentsVolumeViewer.class.getName() );
			if ( volumeViewer != null )
			{
				volumeViewer.setVoxelSpacing( voxelSpacing );
				if ( repaint )
					volumeViewer.updateView( true );
			}
		}
	}
}
