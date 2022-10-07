package org.embl.mobie.viewer.image;

import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.source.Masked;
import org.embl.mobie.viewer.source.SourcePair;

public interface Image< T > extends Masked
{
	SourcePair< T > getSourcePair();

	String getName();

	void transform( AffineTransform3D affineTransform3D );
}
