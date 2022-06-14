package org.embl.mobie.viewer.source;

import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;

public class LazyConverter< T extends NumericType< T > > implements Converter< T, ARGBType >
{
	private final SourceAndConverterAndTables< T > sourceAndConverter;

	public LazyConverter( SourceAndConverterAndTables< T > sourceAndConverter )
	{
		this.sourceAndConverter = sourceAndConverter;
	}

	@Override
	public void convert( T input, ARGBType output )
	{
		sourceAndConverter.getSourceAndConverter().getConverter().convert( input, output );
	}
}
