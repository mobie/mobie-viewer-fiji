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
package org.embl.mobie.lib.serialize;

/**
 * Declares a derived image source that wraps an annotation (segmentation/spot) label image
 * and maps each pixel to a numeric value from a table column.
 * <p>
 * In dataset.json, this creates an {@code Image<DoubleType>} where each pixel belonging to
 * an annotation carries that annotation's value for the specified column. Background pixels
 * are zero.
 * <p>
 * Example JSON:
 * <pre>{@code
 * "nuclei-mean_intensity": {
 *   "numericAnnotation": {
 *     "annotationSource": "nuclei",
 *     "column": "mean_intensity"
 *   }
 * }
 * }</pre>
 * <p>
 * The referenced {@code annotationSource} is initialised automatically by
 * {@code ViewManager.initData()} (which now also loads the base annotation sources of
 * numeric annotations before expansion), so it does not need to be listed in the view
 * itself. Expansion happens after all base sources have been initialised.
 *
 * <p>
 * Sample view (view.json) that renders the numeric annotation as an image layer:
 * <pre>{@code
 * {
 *   "sourceDisplays": [
 *     { "segmentationDisplay": { "sources": [ "nuclei" ] } },
 *     {
 *       "imageDisplay": {
 *         "sources": [ "nuclei-anchor_z" ],
 *         "color": "viridis",
 *         "contrastLimits": [ 0, 200 ]
 *       }
 *     }
 *   ]
 * }
 * }</pre>
 */
public class NumericAnnotationDataSource extends AbstractDataSource
{
	/**
	 * Name of an existing annotation (segmentation/spot) source
	 * whose label image provides the pixel mask.
	 */
	public String annotationSource;

	/**
	 * Name of the numeric column in the annotation source's table.
	 * Each pixel belonging to an annotation will carry that annotation's
	 * value from this column.
	 */
	public String column;

	public NumericAnnotationDataSource()
	{
		super();
	}

	public NumericAnnotationDataSource( String name, String annotationSource, String column )
	{
		super( name );
		this.annotationSource = annotationSource;
		this.column = column;
	}
}
