package explore;

import java.net.URI;

public class TestResolveRelativeURL
{
	public static void main( String[] args )
	{
		final URI path = URI.create( "https://data/0.2.1/tableA.csv" );
		final URI link = URI.create( "../0.2.0/tableB.csv" );

		final URI resolve = path.resolve( link );
		System.out.println( "Resolve: " + resolve );

		final URI normalize = resolve.normalize();
		System.out.println( "Normalize: " + normalize );

	}
}
