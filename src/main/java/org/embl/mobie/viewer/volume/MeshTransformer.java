package org.embl.mobie.viewer.volume;

import net.imglib2.realtransform.AffineTransform3D;

public class MeshTransformer
{
	public static float[] transform( float[] mesh, AffineTransform3D transform3D )
	{
		final float[] transformedMesh = new float[ mesh.length ];

		final float[] vertex = new float[ 3 ];
		final float[] transformedVertex = new float[ 3 ];

		for ( int i = 0; i < mesh.length; i+=3 )
		{
			for ( int d = 0; d < 3; d++ )
				vertex[ d ] = mesh[ i + d ];

			transform3D.apply( vertex, transformedVertex );

			for ( int d = 0; d < 3; d++ )
				transformedMesh[ i + d ] = transformedVertex[ d ];
		}

		return transformedMesh;
	}
}
