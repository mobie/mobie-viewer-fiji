package de.embl.cba.mobie.transform;

import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbstractSourceTransformer< T extends NumericType< T > > implements SourceTransformer< T >
{
	protected String name;
	protected transient Map< String, AffineTransform3D > sourceNameToTransform = new HashMap();

	@Override
	public List< SourceAndConverter< T > > transform( List< SourceAndConverter< T > > sourceAndConverters )
	{
		return null;
	}

	@Override
	public AffineTransform3D getTransform( String sourceName )
	{
		return sourceNameToTransform.get( sourceName );
	}
}
