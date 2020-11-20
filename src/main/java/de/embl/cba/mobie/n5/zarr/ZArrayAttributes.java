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

import org.janelia.saalfeldlab.n5.RawCompression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;


/**
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 *
 */
public class ZArrayAttributes {

	protected static final String zarrFormatKey = "zarr_format";
	protected static final String shapeKey = "shape";
	protected static final String chunksKey = "chunks";
	protected static final String dTypeKey = "dtype";
	protected static final String compressorKey = "compressor";
	protected static final String fillValueKey = "fill_value";
	protected static final String orderKey = "order";
	protected static final String filtersKey = "filters";

	private final int zarr_format;
	private final long[] shape;
	private final int[] chunks;
	private final DType dtype;
	private final ZarrCompressor compressor;
	private final String fill_value;
	private final char order;
	private final List<Filter> filters = new ArrayList<>();

	public ZArrayAttributes(
			final int zarr_format,
			final long[] shape,
			final int[] chunks,
			final DType dtype,
			final ZarrCompressor compressor,
			final String fill_value,
			final char order,
			final Collection<Filter> filters) {

		this.zarr_format = zarr_format;
		this.shape = shape;
		this.chunks = chunks;
		this.dtype = dtype;
		this.compressor = compressor == null ? new ZarrCompressor.Raw() : compressor;
		this.fill_value = fill_value;
		this.order = order;
		if (filters != null)
			this.filters.addAll(filters);
	}

	public ZarrDatasetAttributes getDatasetAttributes() {

		final boolean isRowMajor = order == 'C';
		final long[] dimensions = shape.clone();
		final int[] blockSize = chunks.clone();

		if (isRowMajor) {
			Utils.reorder(dimensions);
			Utils.reorder(blockSize);
		}

		return new ZarrDatasetAttributes(
				dimensions,
				blockSize,
				dtype,
				compressor.getCompression(),
				isRowMajor,
				fill_value);
	}

	public long[] getShape() {

		return shape;
	}

	public int getNumDimensions() {

		return shape.length;
	}

	public int[] getChunks() {

		return chunks;
	}

	public ZarrCompressor getCompressor() {

		return compressor;
	}

	public DType getDType() {

		return dtype;
	}

	public int getZarrFormat() {

		return zarr_format;
	}

	public char getOrder() {

		return order;
	}

	public String getFillValue() {

		return fill_value;
	}

	public HashMap< String, Object > asMap() {

		final HashMap< String, Object > map = new HashMap<>();

		map.put(zarrFormatKey, zarr_format);
		map.put(shapeKey, shape);
		map.put(chunksKey, chunks);
		map.put(dTypeKey, dtype.toString());
		map.put(compressorKey, compressor instanceof RawCompression ? null : compressor);
		map.put(fillValueKey, fill_value);
		map.put(orderKey, order);
		map.put(filtersKey, filters);

		return map;
	}

	public Collection<Filter> getFilters() {

		return filters;
	}
}
