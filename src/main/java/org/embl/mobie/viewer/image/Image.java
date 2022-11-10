package org.embl.mobie.viewer.image;

import bdv.viewer.TransformListener;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.select.Listeners;
import org.embl.mobie.viewer.select.SelectionListener;
import org.embl.mobie.viewer.source.Masked;
import org.embl.mobie.viewer.source.SourcePair;

public interface Image< T > extends Masked
{
	Listeners.SynchronizedList< ImageListener > listeners = new Listeners.SynchronizedList<>( );

	SourcePair< T > getSourcePair();

	String getName();

	void transform( AffineTransform3D affineTransform3D );

	default Listeners< ImageListener > listeners()
	{
		return listeners;
	}
}
