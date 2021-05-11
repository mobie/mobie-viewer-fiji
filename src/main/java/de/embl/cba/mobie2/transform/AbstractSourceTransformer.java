package de.embl.cba.mobie2.transform;

import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbstractSourceTransformer implements SourceTransformer
{
	protected transient Map< String, AffineTransform3D > sourceNameToTransform = new HashMap();

	@Override
	public List< SourceAndConverter< ? > > transform( List< SourceAndConverter< ? > > sourceAndConverters )
	{
		return null;
	}

	@Override
	public AffineTransform3D getTransform( String sourceName )
	{
		return sourceNameToTransform.get( sourceName );
	}
}
