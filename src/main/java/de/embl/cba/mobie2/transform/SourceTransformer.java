package de.embl.cba.mobie2.transform;

import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;

import javax.xml.transform.Source;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface SourceTransformer
{
	List< SourceAndConverter< ? > > transform( List< SourceAndConverter< ? > > sourceAndConverters  );

	AffineTransform3D getTransform( String name );
}
