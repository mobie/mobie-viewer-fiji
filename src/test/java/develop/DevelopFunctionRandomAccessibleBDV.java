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
package develop;

import bdv.util.BdvFunctions;
import net.imglib2.FinalInterval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.view.Views;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class DevelopFunctionRandomAccessibleBDV
{
	public static void main( String[] args )
	{
		final FunctionRandomAccessible< UnsignedIntType > fra = new FunctionRandomAccessible<>( 3, new ValueProvider(), UnsignedIntType::new );

		final RandomAccessibleInterval< UnsignedIntType > rai = Views.interval( fra, new FinalInterval( 100, 100, 100 ) );

		BdvFunctions.show( rai, "" );
	}

	static class ValueProvider implements BiConsumer< Localizable, UnsignedIntType >
	{
		private static AtomicInteger instance = new AtomicInteger( 0 );

		public ValueProvider()
		{
			System.out.println( "" + instance.incrementAndGet() );
		}

		@Override
		public void accept( Localizable localizable, UnsignedIntType unsignedIntType )
		{
			unsignedIntType.set( ( int ) ( Math.random() * 65000 ) );
		}
	}
}
