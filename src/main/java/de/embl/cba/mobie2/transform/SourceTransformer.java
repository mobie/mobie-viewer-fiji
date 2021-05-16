package de.embl.cba.mobie2.transform;

import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import sun.text.normalizer.UCharacter;

import javax.xml.transform.Source;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface SourceTransformer< T extends NumericType< T > >
{
	List< SourceAndConverter< T > > transform( List< SourceAndConverter< T > > sourceAndConverters  );

	AffineTransform3D getTransform( String name );
}
