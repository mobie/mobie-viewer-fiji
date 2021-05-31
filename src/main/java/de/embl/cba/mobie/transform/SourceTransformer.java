package de.embl.cba.mobie.transform;

import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;

import java.util.List;

public interface SourceTransformer< T extends NumericType< T > >
{
	List< SourceAndConverter< T > > transform( List< SourceAndConverter< T > > sourceAndConverters  );

	AffineTransform3D getTransform( String name );
}
