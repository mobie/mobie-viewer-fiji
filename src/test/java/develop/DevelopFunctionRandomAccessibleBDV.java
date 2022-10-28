package develop;

import bdv.util.BdvFunctions;
import net.imglib2.FinalInterval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class DevelopFunctionRandomAccessibleBDV
{
	public static void main( String[] args )
	{
		final FunctionRandomAccessible< UnsignedIntType > fra = new FunctionRandomAccessible<>( 3, new ValueProvider(), UnsignedIntType::new );

		final RandomAccessibleInterval< UnsignedIntType > rai = Views.interval( fra, new FinalInterval( 100, 100, 100 ) );

		BdvFunctions.show( rai, "" );
	}

	static class ValueProvider implements BiConsumer< Localizable, UnsignedIntType >
	{
		private static AtomicInteger instance = new AtomicInteger( 0 );

		public ValueProvider()
		{
			System.out.println( "" + instance.incrementAndGet() );
		}

		@Override
		public void accept( Localizable localizable, UnsignedIntType unsignedIntType )
		{
			unsignedIntType.set( ( int ) ( Math.random() * 65000 ) );
		}
	}
}
