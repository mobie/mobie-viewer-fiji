package develop;

import net.imglib2.realtransform.AffineTransform3D;

public class DevelopInverseTransform
{
	public static void main( String[] args )
	{
		final AffineTransform3D a = new AffineTransform3D();
		a.translate( 10, 0, 0 );
		a.scale( 10, 1, 1 );

		final AffineTransform3D b = new AffineTransform3D();
		b.translate( 100, 1, 1 );

		AffineTransform3D c = a.copy().concatenate( b ); // full

		AffineTransform3D d = c.copy().concatenate( a.inverse() );
		AffineTransform3D d2 = c.copy().preConcatenate( a.inverse() );

		int sdsf = 1;

	}
}
