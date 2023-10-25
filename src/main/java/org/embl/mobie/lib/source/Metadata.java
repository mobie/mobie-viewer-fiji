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

import ij.IJ;
import ij.ImagePlus;

public class Metadata
{
	public String color = "White";

	public double[] contrastLimits = null;
	public Integer numTimePoints = null;
	public Integer numZSlices = 1;
	public Integer numChannelsContainer = 1; // in MoBIE each image has jsut one channel, but the container could have multiple

	public Metadata()
	{
	}

	public Metadata( ImagePlus imagePlus )
	{
		color = "White"; // TODO: why not extract the color?
		// we could use more direct methods:
		// https://imagej.nih.gov/ij/developer/source/ij/plugin/ContrastEnhancer.java.html
		IJ.run( imagePlus, "Enhance Contrast", "saturated=0.35" );
		contrastLimits = new double[]{ imagePlus.getDisplayRangeMin(), imagePlus.getDisplayRangeMax() };
		numTimePoints = imagePlus.getNFrames();
		numZSlices = imagePlus.getNSlices();
		numChannelsContainer = imagePlus.getNChannels();
	}

}
