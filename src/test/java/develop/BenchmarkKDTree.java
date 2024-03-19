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

import net.imglib2.KDTree;
import net.imglib2.RealPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

public class BenchmarkKDTree
{
	public static void main( String[] args ) throws IOException
	{
		final boolean b = ( float ) ( 0.0 + 1e-7 * 0.1 ) == ( float ) ( 0.0 + 1e-7 * 0.2 );

		final Random r = new Random();
		final float x1 = ( float ) ( 1.0 + 1e-7 * r.nextDouble() );
		final float x2 = ( float ) ( 1.0 + 1e-7 * r.nextDouble() );
		System.out.println( x1 );
		System.out.println( x2);
		System.out.println( x1 - x2 );


		final Map<String, Supplier<RealPoint> > approaches = new LinkedHashMap<>();
//		approaches.put("Random", () -> new RealPoint( r.nextDouble(), r.nextDouble(), r.nextDouble() ));
//		approaches.put("Constant 1.0", () -> new RealPoint( r.nextDouble(), r.nextDouble(), 1.0 ));
//		approaches.put("Constant 0.5", () -> new RealPoint( r.nextDouble(), r.nextDouble(), 0.5 ));
//		approaches.put("Constant 0.0", () -> new RealPoint( r.nextDouble(), r.nextDouble(), 0.0 ));

		approaches.put("Perturbed 1.0", () -> new RealPoint( r.nextDouble(), r.nextDouble(), 1.0 + 1e-5*r.nextDouble() ));
		approaches.put("Perturbed 0.5", () -> new RealPoint( r.nextDouble(), r.nextDouble(), 0.5 + 1e-5*r.nextDouble() ));
		approaches.put("Perturbed 0.0", () -> new RealPoint( r.nextDouble(), r.nextDouble(), 0.0 + 1e-5*r.nextDouble() ));


		for (int iter=1; iter<=1; iter++) {
			System.out.println("Iteration #" + iter + ":");
			for (final String approach : approaches.keySet()) {
				final Supplier<RealPoint> supplier = approaches.get(approach);
				final List<RealPoint> points = new ArrayList<>();
				for (int i = 0; i < 100000; i++)
					points.add(supplier.get());

				long start = System.currentTimeMillis();
				new KDTree<>(points, points);
				System.out.println("--> " + approach + ": build tree in " + (System.currentTimeMillis() - start));
			}
		}

//		Random r = new Random();
//		List< RealPoint > points = new ArrayList<>();
//		for ( int i = 0; i < 100000; i++ )
//			points.add( new RealPoint( r.nextDouble(), r.nextDouble(), r.nextDouble() ) );
//
//		long start = System.currentTimeMillis();
//		new KDTree<>( points, points );
//		System.out.println( "Build tree in " + ( System.currentTimeMillis() - start ) );
//
//		points = new ArrayList<>();
//		for ( int i = 0; i < 100000; i++ )
//			points.add( new RealPoint( r.nextDouble(), r.nextDouble(), 1.0 ) );
//
//		start = System.currentTimeMillis();
//		new KDTree<>( points, points );
//		System.out.println( "Build tree in " + ( System.currentTimeMillis() - start ) );

	}
}
