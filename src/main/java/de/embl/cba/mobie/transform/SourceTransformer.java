package de.embl.cba.mobie.transform;

import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;

import java.util.List;
import java.util.Map;

public interface SourceTransformer< T extends NumericType< T > >
{
	void transform( Map< String, SourceAndConverter< T > > sourceNameToSourceAndConverter );

	//AffineTransform3D getTransform( String name );

	/**
	 *
	 * @return a list of the names of all sources that should be transformed using this transformer.
	 */
	List< String > getSources();
}
