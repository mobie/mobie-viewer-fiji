package playground;

import net.imglib2.realtransform.AffineTransform3D;

import java.awt.geom.AffineTransform;

public class TryAffine3DPrint
{
	public static void main( String[] args )
	{
		final AffineTransform3D affineTransform = new AffineTransform3D();
		final String view = affineTransform.toString().replace( "3d-affine", "view" );
		System.out.println( view );
	}
}
