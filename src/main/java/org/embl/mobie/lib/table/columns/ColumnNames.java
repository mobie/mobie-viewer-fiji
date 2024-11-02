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
package org.embl.mobie.lib.table.columns;

public class ColumnNames
{
	public static final String LABEL_IMAGE_ID = "label_image_id";
	public static final String LABEL_ID = "label_id";
	public static final String TIMEPOINT = "timepoint";
	// TODO make this an array
	public static final String ANCHOR_X = "anchor_x";
	public static final String ANCHOR_Y = "anchor_y";
	public static final String ANCHOR_Z = "anchor_z";
	// TODO make this an array
	public static final String BB_MIN_X = "bb_min_x";
	public static final String BB_MIN_Y = "bb_min_y";
	public static final String BB_MIN_Z = "bb_min_z";
	// TODO make this an array
	public static final String BB_MAX_X = "bb_max_x";
	public static final String BB_MAX_Y = "bb_max_y";
	public static final String BB_MAX_Z = "bb_max_z";

	public static final String REGION_ID = "region_id";

	// TODO make this an array
	public static final String SPOT_X = "x";
	public static final String SPOT_Y = "y";
	public static final String SPOT_Z = "z";
	public static final String SPOT_ID = "spot_id";

	// for multi-well plates
	public static final String ROW_INDEX = "row_index";
	public static final String COLUMN_INDEX = "column_index";

}
