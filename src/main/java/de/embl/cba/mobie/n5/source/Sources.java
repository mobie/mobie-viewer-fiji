package de.embl.cba.mobie.n5.source;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvStackSource;
import bdv.viewer.SourceAndConverter;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Cast;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import java.lang.reflect.Method;

public abstract class Sources
{
	public static < R extends NumericType< R > & RealType< R > > SourceAndConverter< R > replaceConverter( SourceAndConverter< ? > source, Converter< RealType, ARGBType > converter )
	{
		LabelSource< R > labelVolatileSource = new LabelSource( source.asVolatile().getSpimSource() );
		SourceAndConverter volatileSourceAndConverter = new SourceAndConverter( labelVolatileSource , converter );
		LabelSource< R > labelSource = new LabelSource( source.getSpimSource() );
		SourceAndConverter sourceAndConverter = new SourceAndConverter( labelSource, converter, volatileSourceAndConverter );
		return sourceAndConverter;
	}

	public static < R extends NumericType< R > & RealType< R > > BdvStackSource< R > showAsLabelMask( BdvStackSource< ? > bdvStackSource )
	{
		LabelConverter converter = new LabelConverter();
		SourceAndConverter< R > sac = replaceConverter( bdvStackSource.getSources().get( 0 ), converter );
		BdvHandle bdvHandle = bdvStackSource.getBdvHandle();
		bdvStackSource.removeFromBdv();

		// access by reflection, which feels quite OK as this will be public in future versions of bdv anyway:
		// https://github.com/bigdataviewer/bigdataviewer-vistools/commit/8cad3edac6c563dc2d22abf71345655afa7f49cc
		try
		{
			Method method = BdvFunctions.class.getDeclaredMethod("addSpimDataSource", BdvHandle.class, SourceAndConverter.class, int.class );
			method.setAccessible( true );
			BdvStackSource< R > newBdvStackSource = (BdvStackSource< R >) method.invoke( "addSpimDataSource", bdvHandle, sac, 1 );

			Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
			behaviours.install( bdvHandle.getTriggerbindings(), "label source " + sac.getSpimSource().getName() );
			behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
							new Thread( () -> {
								converter.getColoringModel().incRandomSeed();
								bdvHandle.getViewerPanel().requestRepaint();
							} ).start(),
					"shuffle random colors " + sac.getSpimSource().getName(),
					"ctrl L" );

			return newBdvStackSource;
		}
		catch ( Exception e )
		{
			throw new RuntimeException( e );
		}


	}

	/**
	 * TODO: add time
	 *
	 * @param bdvStackSource
	 * @param level
	 */
	public static void viewAsHyperstack( BdvStackSource< ? > bdvStackSource, int level )
	{
		RandomAccessibleInterval< ? > rai = bdvStackSource.getSources().get( 0 ).getSpimSource().getSource( 0, level );
		IntervalView< ? > permute = Views.permute( Views.addDimension( rai, 0, 0 ), 2, 3 );
		ImageJFunctions.wrap( Cast.unchecked( permute ), "em" ).show();
	}
}
