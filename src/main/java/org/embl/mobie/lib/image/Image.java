package org.embl.mobie.lib.image;

import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.lib.select.Listeners;
import org.embl.mobie.lib.source.Masked;
import org.embl.mobie.lib.source.SourcePair;

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
