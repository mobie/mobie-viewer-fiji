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
package develop;

import net.imglib2.type.numeric.integer.UnsignedIntType;
import org.embl.mobie.viewer.SourceNameEncoder;

public class DevelopSourceNameBitEncoding
{
	public static void main( String[] args )
	{
		final UnsignedIntType unsignedIntType = new UnsignedIntType();
		unsignedIntType.set( 1024 );

		final long label = unsignedIntType.get();
		System.out.println(Long.toBinaryString((long)label));

		final long image = 10;
		final long l1 = image << 16;
		System.out.println(l1);
		System.out.println(Long.toBinaryString(l1));
		final long both = label + ( image << 16 );
		System.out.println(Long.toBinaryString(both));
		// image
		final long decodedImage = both >> 16;
		System.out.println(Long.toBinaryString( decodedImage ));
		System.out.println("image id: " + decodedImage);
		// label
		final long decodedLabel = both & 0x0000FFFF;
		System.out.println(Long.toBinaryString( decodedLabel ));
		System.out.println("label id: " + decodedLabel);

		System.out.printf( "----------------------\n" );

		SourceNameEncoder.addName( "hello" );
		SourceNameEncoder.addName( "world" );
		final UnsignedIntType labelIndex = new UnsignedIntType( 133 );
		SourceNameEncoder.encodeName( labelIndex, "hello" );
		System.out.println(labelIndex.get());
		System.out.println("name: " + SourceNameEncoder.getName( labelIndex ));
		System.out.println("value: " + SourceNameEncoder.getValue( labelIndex ));

	}
}
