package develop;

import bdv.util.AxisOrder;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.util.volatiles.VolatileViews;
import bdv.viewer.Source;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.bioimageanalysis.icy.deeplearning.Model;
import org.bioimageanalysis.icy.deeplearning.tensor.Tensor;
import org.bioimageanalysis.icy.deeplearning.utils.EngineInfo;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.SpimDataOpener;

import java.util.ArrayList;
import java.util.List;

public class DevelopDeepPlatySegmentation
{
	public static void main( String[] args ) throws Exception
	{
		// Open and show the 8 TB input data
		//
		final AbstractSpimData platySBEM = new SpimDataOpener().openSpimData( "https://s3.embl.de/i2k-2020/platy-raw.ome.zarr", ImageDataFormat.OmeZarr );
		final BdvStackSource< ? > stackSource = BdvFunctions.show( platySBEM ).get( 0 );
		final BdvHandle bdvHandle = stackSource.getBdvHandle();
		final Source< ? > source = stackSource.getSources().get( 0 ).getSpimSource();

		// Create the lazy prediction image
		//
		final RandomAccessibleInterval< ? > scale3 = source.getSource( 0, 3 );
		RandomAccessibleInterval< FloatType > prediction =
				new ReadOnlyCachedCellImgFactory().create(
						scale3.dimensionsAsLongArray(),
						new FloatType(),
						new PredictionLoader( loadModel(), scale3 ),
						ReadOnlyCachedCellImgOptions.options().cellDimensions( 256, 256, 32 )
				);

		// Show the predictions
		//
		final RandomAccessibleInterval< Volatile< FloatType > > volatilePredictions = VolatileViews.wrapAsVolatile( prediction );
		BdvFunctions.show( volatilePredictions, "prediction", BdvOptions.options().axisOrder( AxisOrder.XYZ ).addTo( bdvHandle ) );
	}

	private static Model loadModel()
	{
		try
		{
			String engine = "torchscript";
			String engineVersion = "1.9.1";
			String enginesDir = "/Users/tischer/Desktop/deep-engines";
			String modelFolder = "/Users/tischer/Desktop/deep-models/platynereisemnucleisegmentationboundarymodel_torchscript";
			String modelSource = modelFolder + "/weights-torchscript.pt";
			boolean cpu = true;
			boolean gpu = true;
			EngineInfo engineInfo = EngineInfo.defineDLEngine( engine, engineVersion, enginesDir, cpu, gpu );
			Model model = Model.createDeepLearningModel( modelFolder, modelSource, engineInfo );
			model.loadModel();
			return model;
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}

	static class PredictionLoader implements CellLoader< FloatType >
	{
		private final Model model;
		private final RandomAccessibleInterval< ? > input;

		public PredictionLoader( Model model, RandomAccessibleInterval< ? > input )
		{

			this.model = model;
			this.input = input;
		}

		@Override
		public void load( SingleCellArrayImg< FloatType, ? > cell )
		{
			final RandomAccessibleInterval< ? > crop = Views.interval( input, cell );

			// Fix data type (all models need float as input)
			//
			RandomAccessibleInterval< FloatType > rai = Tensor.createCopyOfRaiInWantedDataType( (RandomAccessibleInterval) crop, new FloatType() );

			// Fix dimension order
			//
			final long[] loadedDims = rai.dimensionsAsLongArray();
			rai = Views.addDimension( rai, 0, 0 );
			rai = Views.moveAxis( rai, rai.numDimensions()-1, 0 );
			rai = Views.addDimension( rai, 0, 0 );
			rai = Views.moveAxis( rai, rai.numDimensions()-1, 0 );
			final long[] convertedDims = rai.dimensionsAsLongArray();

			// Build input tensor
			//
			Tensor< FloatType > inputTensor = Tensor.build("input0", "bczyx", rai);
			List< Tensor< ? > > inputs = new ArrayList<Tensor<?>>();
			inputs.add( inputTensor );

			// We need to specify the output tensors with its axes order
			// and name, but empty
			final Tensor< T > outputTensor = Tensor.buildEmptyTensor( "output0", "bczyx" );
			List<Tensor<?>> outputs = new ArrayList<Tensor<?>>();
			outputs.add(outTensor);
			outputs = model.runModel(inputs, outputs);
			System.out.println( "Created outputs: " + outputs.size() );
			RandomAccessibleInterval< ? > output = outputs.get( 0 ).getData();
			final long[] outputDims = output.dimensionsAsLongArray();
		}
	}
}
