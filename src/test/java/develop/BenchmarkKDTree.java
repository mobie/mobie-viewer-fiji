package develop;

import net.imglib2.KDTree;
import net.imglib2.RealPoint;
import org.embl.mobie.viewer.annotation.AnnotatedSpot;
import org.embl.mobie.viewer.table.DefaultAnnotatedSpot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class BenchmarkKDTree
{
	public static void main( String[] args ) throws IOException
	{
		Random r = new Random();
		List< RealPoint > points = new ArrayList<>();
		for ( int i = 0; i < 100000; i++ )
			points.add( new RealPoint( r.nextDouble(), r.nextDouble(), r.nextDouble() ) );

		long start = System.currentTimeMillis();
		new KDTree<>( points, points );
		System.out.println( "Build tree in " + ( System.currentTimeMillis() - start ) );

		points = new ArrayList<>();
		for ( int i = 0; i < 100000; i++ )
			points.add( new RealPoint( r.nextDouble(), r.nextDouble(), 1.0 ) );

		start = System.currentTimeMillis();
		new KDTree<>( points, points );
		System.out.println( "Build tree in " + ( System.currentTimeMillis() - start ) );

	}
}
