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
package org.embl.mobie.lib.transform;

import net.imglib2.realtransform.RealTransform;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ElastixBSplineRealTransformCreator
{
	private static final String ELASTIX_TRANSFORM_CLASS = "itc.transforms.elastix.ElastixTransform";
	private static final String ELASTIX_BSPLINE_CLASS = "itc.transforms.elastix.ElastixBSplineTransform";
	private static final String ELASTIX_CONVERTER_CLASS = "itc.converters.ElastixBSplineToBSplineRealTransform";

	private ElastixBSplineRealTransformCreator()
	{
	}

	public static RealTransform create( String transformParametersFile )
	{
		try
		{
			final Class< ? > elastixTransformClass = Class.forName( ELASTIX_TRANSFORM_CLASS );
			final Method loadMethod = elastixTransformClass.getMethod( "load", File.class );
			final Object elastixTransform = loadMethod.invoke( null, new File( transformParametersFile ) );

			final Field transformField = elastixTransformClass.getField( "Transform" );
			final String transformType = ( String ) transformField.get( elastixTransform );
			if ( ! "BSplineTransform".equals( transformType ) )
				throw new UnsupportedOperationException( "Expected an Elastix BSpline transform but got: " + transformType );

			final Class< ? > elastixBSplineClass = Class.forName( ELASTIX_BSPLINE_CLASS );
			final Class< ? > elastixConverterClass = Class.forName( ELASTIX_CONVERTER_CLASS );
			final Method convertMethod = elastixConverterClass.getMethod( "convert", elastixBSplineClass );

			// The Elastix parameter file is assumed to already use the same physical units as the source.
			return ( RealTransform ) convertMethod.invoke( null, elastixTransform );
		}
		catch ( Exception e )
		{
			throw new RuntimeException( "Could not create Elastix BSpline transform from: " + transformParametersFile, e );
		}
	}
}
