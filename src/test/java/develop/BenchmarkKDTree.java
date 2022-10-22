package develop;

import net.imglib2.KDTree;
import net.imglib2.RealPoint;
import org.embl.mobie.viewer.annotation.AnnotatedSpot;
import org.embl.mobie.viewer.table.DefaultAnnotatedSpot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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
