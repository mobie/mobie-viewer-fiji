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

import org.embl.mobie.MoBIE;
import org.embl.mobie.command.CommandConstants;

import org.scijava.Initializable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Configure BigVolumeViewer Rendering")
public class ConfigureBVVRenderingCommand implements BdvPlaygroundActionCommand, Initializable
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter( label = "Render width" )
	public int renderWidth = 600;

	@Parameter( label = "Render height" )
	public int renderHeight = 600;

	@Parameter( label = "Dither window size",
			choices = { "none (always render full resolution)", "2x2", "3x3", "4x4", "5x5", "6x6", "7x7", "8x8" } )
	public String dithering = "3x3";

	@Parameter( label = "Number of dither samples",
			description = "Pixels are interpolated from this many nearest neighbors when dithering. This is not very expensive, it's fine to turn it up to 8.",
			min="1",
			max="8",
			style="slider")
	public int numDitherSamples = 3;

	@Parameter( label = "GPU cache tile size" )
	public int cacheBlockSize = 32;

	@Parameter( label = "GPU cache size (in MB)",
				description = "The size of the GPU cache texture will match this as close as possible with the given tile size." )
	public int maxCacheSizeInMB = 500;

	@Parameter( label = "Camera distance",
				description = "Distance from camera to z=0 plane. In units of pixel width." )
	public double dCam = 3000;

	@Parameter( label = "Clip distance far",
	description = "Visible depth from z=0 further away from the camera. In units of pixel width.")
	public double dClipFar = 1000;
	
	@Parameter( label = "Clip distance near",
			description = "Visible depth from z=0 closer to the camera. In units of pixel width. MUST BE SMALLER THAN CAMERA DISTANCE!")
	public double dClipNear = 1000;

	
	@Override
	public void run()
	{
		MoBIE.getInstance().getViewManager().getBigVolumeViewer().updateBVVRenderSettings();
	}
}
