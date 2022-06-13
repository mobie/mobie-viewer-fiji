package org.embl.mobie.viewer.source;

import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;

public class VolatileLazyConverter< T extends NumericType< T >, V extends Volatile< T > > implements Converter< V, ARGBType >
{
	private final LazySourceAndConverterAndTables< T > sourceAndConverter;

	public VolatileLazyConverter( LazySourceAndConverterAndTables< T > sourceAndConverter )
	{
		this.sourceAndConverter = sourceAndConverter;
	}

	@Override
	public void convert( V input, ARGBType output )
	{
		final Converter< V, ARGBType > converter = ( Converter< V, ARGBType > ) sourceAndConverter.openSourceAndConverter().asVolatile().getConverter();
		converter.convert( input, output );
	}
}
