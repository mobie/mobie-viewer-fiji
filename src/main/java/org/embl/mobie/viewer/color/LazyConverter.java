package org.embl.mobie.viewer.color;

import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import org.embl.mobie.viewer.source.SourceAndConverterAndTables;

public class LazyConverter< N extends RealType< N > > extends AbstractLazyConverter< N > implements RealARGBColorConverter< N >
{
	private Converter< N, ARGBType > converter;
	private ColorConverter colorConverter;

	public LazyConverter( SourceAndConverterAndTables< N > nSourceAndConverterAndTables )
	{
		super( nSourceAndConverterAndTables );
	}

	@Override
	public void convert( N input, ARGBType output )
	{
		getConverter().convert( input, output );
	}

	@Override
	protected Converter< N, ARGBType > getConverter()
	{
		if ( converter == null )
		{
			converter = sourceAndConverterAndTables.getSourceAndConverter().getConverter();
		}
		return converter;
	}

}
