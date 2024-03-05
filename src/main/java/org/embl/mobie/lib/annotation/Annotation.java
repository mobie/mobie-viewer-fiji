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
package org.embl.mobie.lib.annotation;

import net.imglib2.realtransform.AffineTransform3D;

public interface Annotation extends Location
{
	String uuid();

	// Data source (can be a table or a label mask image)
	String source();

	// Integer label for representing the annotation as a
	// region in one time point of a label image
	int label();

	// For retrieving features (measurements)
	// (typically: feature = column in an annotation table)
	Object getValue( String feature );

	// For retrieving numerical features (measurements)
	// (typically: feature = column in an annotation table)
	Double getNumber( String feature );

	// For adding text annotations
	void setString( String columnName, String value );

	// For adding numeric annotations
	void setNumber( String columnName, double value );

	// Transform the spatial coordinates of this annotation.
	// Note that there are also methods to transform annotations,
	// which create a copy of the annotation;
	// e.g. {@code AffineTransformedAnnotatedSegment};
	// use those methods if you need both the transformed and
	// untransformed annotations.
	void transform( AffineTransform3D affineTransform3D );
}
