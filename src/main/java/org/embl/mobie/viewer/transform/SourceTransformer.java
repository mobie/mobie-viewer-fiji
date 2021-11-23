package org.embl.mobie.viewer.transform;

import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;

import java.util.List;
import java.util.Map;

public interface SourceTransformer
{
	void transform( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter );

	/**
	 * @return names of all sources that should be transformed using this transformer.
	 */
	List< String > getSources();
}
