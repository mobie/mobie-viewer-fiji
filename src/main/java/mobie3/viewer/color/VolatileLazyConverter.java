package mobie3.viewer.color;

import mobie3.viewer.source.SourceAndConverterAndTables;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;

public class VolatileLazyConverter< N extends NumericType< N >, V extends Volatile< N > > extends AbstractLazyConverter< N > implements Converter< V, ARGBType >, ColorConverter
{
	private Converter< V, ARGBType > converter;
	private ColorConverter colorConverter;

	public VolatileLazyConverter( SourceAndConverterAndTables< N > sourceAndConverterAndTables )
	{
		super( sourceAndConverterAndTables );
	}

	@Override
	protected Converter< V, ARGBType > getConverter()
	{
		if ( converter == null )
		{
			converter = ( Converter< V, ARGBType > ) sourceAndConverterAndTables.getSourceAndConverter().asVolatile().getConverter();
		}
		return converter;
	}


	@Override
	public void convert( V input, ARGBType output )
	{
		getConverter().convert( input, output );
	}
}
