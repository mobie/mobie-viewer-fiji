package mobie3.viewer.color;

import mobie3.viewer.source.SourceAndConverterAndTables;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;

public abstract class AbstractLazyConverter< N extends NumericType< N > >
{
	protected final SourceAndConverterAndTables< N > sourceAndConverterAndTables;

	public AbstractLazyConverter( SourceAndConverterAndTables< N > sourceAndConverterAndTables )
	{
		this.sourceAndConverterAndTables = sourceAndConverterAndTables;
	}

	protected abstract Converter getConverter();

	protected ColorConverter getColorConverter()
	{
		// assumes that the converter is a ColorConverter
		return ( ColorConverter ) getConverter();
	}

	public ARGBType getColor()
	{
		return getColorConverter().getColor();
	}

	public void setColor( ARGBType c )
	{
		getColorConverter().setColor( c );
	}

	public boolean supportsColor()
	{
		return getColorConverter().supportsColor();
	}

	public double getMin()
	{
		return getColorConverter().getMin();
	}

	public double getMax()
	{
		return getColorConverter().getMax();
	}

	public void setMin( double min )
	{
		getColorConverter().setMin( min );
	}

	public void setMax( double max )
	{
		getColorConverter().setMax( max );
	}
}
