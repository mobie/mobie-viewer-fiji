/**
 * Copyright (c) 2019, Stephan Saalfeld
 * All rights reserved.
 *
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
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.embl.cba.mobie.n5.zarr;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DatasetAttributes;


/**
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 *
 */
public class ZarrDatasetAttributes extends DatasetAttributes
{

	private final transient boolean isRowMajor;
	private final transient DType dType;
	private final transient byte[] fillBytes;
	private final transient String fillValue;

	public ZarrDatasetAttributes(
			final long[] dimensions,
			final int[] blockSize,
			final DType dType,
			final Compression compression,
			final boolean isRowMajor,
			final String fill_value) {

		super(dimensions, blockSize, dType.getDataType(), compression);
		this.dType = dType;
		this.isRowMajor = isRowMajor;
		this.fillBytes = dType.createFillBytes(fill_value);
		this.fillValue = fill_value;
	}

	public boolean isRowMajor() {

		return isRowMajor;
	}

	public DType getDType() {

		return dType;
	}

	public byte[] getFillBytes() {

		// TODO Auto-generated method stub
		return fillBytes;
	}

	public String getFillValue() {
		return fillValue;
	}
}
