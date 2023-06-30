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
package develop;

import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;
import net.imagej.ImageJ;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Scale3D;

import java.io.IOException;

public class DevelopNormalisedViewerTransforms
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		testNormalisationAndReversion();

		try {
			final MoBIE moBIE = new MoBIE("https://github.com/mobie-org/covid-em-datasets",
					MoBIESettings.settings() );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void testNormalisationAndReversion()
	{
		final AffineTransform3D affineTransform3D = new AffineTransform3D();
		System.out.println( "Identity: " + affineTransform3D );

		// transform the transform

		// translate
		final AffineTransform3D translation = new AffineTransform3D();
		translation.translate( 10, 10, 0 );
		affineTransform3D.preConcatenate( translation );

		// scale
		final Scale3D scale3D = new Scale3D( 0.1, 0.1, 0.1 );
		affineTransform3D.preConcatenate( scale3D );

		System.out.println( "Normalised translated and scaled: " + affineTransform3D );

		// invert above transformations
		affineTransform3D.preConcatenate( scale3D.inverse() );
		affineTransform3D.preConcatenate( translation.inverse() );

		System.out.println( "Reversed: " + affineTransform3D ); // should be identity again
	}

}
