package org.embl.mobie.viewer.source;

import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;

public class VolatileLazyConverter< T extends NumericType< T >, V extends Volatile< T > > implements Converter< V, ARGBType >
{
	private final SourceAndConverterAndTables< T > sourceAndConverterAndTables;

	public VolatileLazyConverter( SourceAndConverterAndTables< T > sourceAndConverterAndTables )
	{
		this.sourceAndConverterAndTables = sourceAndConverterAndTables;
	}

	@Override
	public void convert( V input, ARGBType output )
	{
		final Converter< V, ARGBType > converter = ( Converter< V, ARGBType > ) sourceAndConverterAndTables.getSourceAndConverter().asVolatile().getConverter();
		converter.convert( input, output );
	}
}
