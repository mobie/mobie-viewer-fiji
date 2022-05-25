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
package org.embl.mobie.viewer.bdv.render;

import com.google.gson.annotations.SerializedName;

/*
 * Constants to define the blending mode of sources.
 * These constants are used within {@link AccumulateMixedProjectorARGB}.
 *
 * Sum:
 * The ARGB values of the source will be added to the final ARGB to be displayed.
 *
 * Average:
 * The ARGB values of all sources with the Average projection mode will first be averaged
 * before being added to the final ARGB to be displayed.
 * This is useful for overlapping electron microscopy data sets.
 *
 * Occluding:
 * For a given pixel, if there are sources with the Exclusive projection mode and with
 * an alpha value larger than zero, only these source will be displayed.
 * The pixels of all other sources will not be visible.
 * This is useful, e.g., if there is a region where one source contains information
 * at a higher resolution than another source. Selecting the Exclusive projection mode
 * can be used to only show this source.
 *
 */
public enum BlendingMode
{
	@SerializedName("sum")
	Sum,
	@SerializedName("sumOccluding")
	SumOccluding,
	@SerializedName("average") @Deprecated
	Average,
	@SerializedName("averageOccluding") @Deprecated
	AverageOccluding;

	// To use in Commands until they can do enums as choices
	public static final String SUM = "Sum";
	public static final String SUM_OCCLUDING = "SumOccluding";

	// To use as a key for the xml
	// underscore necessary for valid xml element to store in @see DisplaySettings
	public static final String BLENDING_MODE = "Blending Mode";

	public static boolean isOccluding( BlendingMode blendingMode )
	{
		return ( blendingMode.equals( BlendingMode.AverageOccluding ) || blendingMode.equals( BlendingMode.SumOccluding ) );
	}
}
