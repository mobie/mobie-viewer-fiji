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
package org.embl.mobie.lib.source;

import ch.epfl.biop.bdv.img.imageplus.ImagePlusToSpimData;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.ContrastEnhancer;
import ij.process.ImageStatistics;

import static ij.measure.Measurements.MIN_MAX;

// FIXME: move this to mobie-io
public class Metadata
{
	public String color = "White";

	public double[] contrastLimits = null;
	public Integer numTimePoints = null;
	public Integer numZSlices = 1;
	public Integer numChannelsContainer = 1; // in MoBIE each image has jsut one channel, but the container could have multiple
	private static boolean logBFError = true;

	public Metadata()
	{
	}

	public Metadata( ImagePlus imagePlus )
	{
		color = "White"; // TODO: why not also extract the color?

		try
		{
			ImageStatistics statistics = ImageStatistics.getStatistics( imagePlus.getProcessor(), MIN_MAX, null );
			new ContrastEnhancer().stretchHistogram( imagePlus.getProcessor(), 0.35, statistics );
		}
		catch ( Exception e )
		{
			if ( logBFError )
			{
				// https://forum.image.sc/t/b-c-for-a-whole-virtual-stack-cont/57811/12
				IJ.log( "[WARNING] Could not determine auto-contrast for some images due to Bio-Formats issue: https://forum.image.sc/t/b-c-for-a-whole-virtual-stack-cont/57811/12" );
				logBFError = false;
			}
		}
		contrastLimits = new double[]{ imagePlus.getProcessor().getMin(), imagePlus.getProcessor().getMax() };
		numTimePoints = imagePlus.getNFrames();
		numZSlices = imagePlus.getNSlices();
		numChannelsContainer = imagePlus.getNChannels();
	}

}
