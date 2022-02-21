package org.embl.mobie.viewer.view;

import bdv.tools.transformation.TransformedSource;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.transform.AffineSourceTransformer;

import java.util.Arrays;

public class ViewHelpers
{
	public static AffineSourceTransformer createAffineSourceTransformer( TransformedSource< ? > transformedSource )
	{
		AffineTransform3D fixedTransform = new AffineTransform3D();
		transformedSource.getFixedTransform( fixedTransform );
		if ( ! fixedTransform.isIdentity() ) {
			return new AffineSourceTransformer(
						"manualTransform",
						fixedTransform.getRowPackedCopy(),
						Arrays.asList( transformedSource.getWrappedSource().getName() ),
						Arrays.asList( transformedSource.getName() ) );
		}
		else
		{
			return null;
		}
	}
}
