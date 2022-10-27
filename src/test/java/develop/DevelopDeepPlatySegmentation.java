package develop;

import bdv.cache.SharedQueue;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.util.RandomAccessibleIntervalMipmapSource;
import bdv.util.VolatileRandomAccessibleIntervalMipmapSource;
import bdv.viewer.Source;
import ij.IJ;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.converter.Converter;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.VolatileFloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import org.bioimageanalysis.icy.deeplearning.Model;
import org.bioimageanalysis.icy.deeplearning.tensor.Tensor;
import org.bioimageanalysis.icy.deeplearning.utils.EngineInfo;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.SpimDataOpener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DevelopDeepPlatySegmentation
{
	public static void main( String[] args ) throws Exception
	{
		final int level = 3;
		final SharedQueue sharedQueue = new SharedQueue( 3 );

		// Open and show the 8 TB input data
		//
		final AbstractSpimData platySBEM = new SpimDataOpener().openSpimData( "https://s3.embl.de/i2k-2020/platy-raw.ome.zarr", ImageDataFormat.OmeZarrS3, sharedQueue );
		final BdvStackSource< ? > stackSource = BdvFunctions.show( platySBEM ).get( 0 );
		final BdvHandle bdvHandle = stackSource.getBdvHandle();
		final Source< ? > source = stackSource.getSources().get( 0 ).getSpimSource();

		final AffineTransform3D viewerTransform = new AffineTransform3D();
		viewerTransform.set( 31.524161974149372,0.0,0.0,-3471.2941398257967,0.0,31.524161974149372,0.0,-3335.2908913145466,0.0,0.0,31.524161974149372,-4567.901470761989 );
		bdvHandle.getViewerPanel().state().setViewerTransform( viewerTransform );
		IJ.wait( 1000 );

		// Create the lazy prediction image
		//
		final RandomAccessibleInterval< ? > inputRAI = source.getSource( 0, level );
		RandomAccessibleInterval< FloatType > prediction =
				new ReadOnlyCachedCellImgFactory().create(
						inputRAI.dimensionsAsLongArray(),
						new FloatType(),
						new Predictor( loadModel(), inputRAI ),
						ReadOnlyCachedCellImgOptions.options().cellDimensions( 256, 256, 32 )
				);


		final AffineTransform3D predictionTransform = new AffineTransform3D();
		source.getSourceTransform( 0, level, predictionTransform  );


		final RandomAccessibleInterval< FloatType >[] predictionRais = new RandomAccessibleInterval[ 1 ];
		predictionRais[ 0 ] = prediction;

		// TODO: Fix the voxel dimensions
		final double[] sourceVoxelSize = source.getVoxelDimensions().dimensionsAsDoubleArray();
		final double[] predictionVoxelSize = new double[ level ];
		predictionTransform.apply( sourceVoxelSize, predictionVoxelSize );
		final FinalVoxelDimensions predictionVoxelDimensions = new FinalVoxelDimensions( "micrometer", predictionVoxelSize );

		final double[][] mipmapScales = new double[ 1 ][ level ];
		mipmapScales[ 0 ] = new double[]{ 1, 1, 1 };
		final RandomAccessibleIntervalMipmapSource predictionSource = new RandomAccessibleIntervalMipmapSource( predictionRais, new FloatType(), mipmapScales, predictionVoxelDimensions, predictionTransform, "prediction" );
		final VolatileRandomAccessibleIntervalMipmapSource vPredictionSource = new VolatileRandomAccessibleIntervalMipmapSource( predictionSource, new VolatileFloatType(), sharedQueue );
		BdvFunctions.show( vPredictionSource, BdvOptions.options().addTo( bdvHandle ) );
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

	static class Predictor< I extends RealType< I > & NativeType< I >, O extends RealType< O > & NativeType< O > >  implements CellLoader< FloatType >
	{
		private final Model model;
		private final RandomAccessibleInterval< I > input;
		private I inputType;

		public Predictor( Model model, RandomAccessibleInterval< I > input )
		{
			this.model = model;
			this.input = input;
			inputType = Util.getTypeFromInterval( input );

			final Converter< I, FloatType > converter = RealTypeConverters.getConverter( inputType, new FloatType() );
		}

		@Override
		public void load( SingleCellArrayImg< FloatType, ? > cell )
		{
			final String cellLocation = Arrays.toString( cell.minAsLongArray() ) + " - " + Arrays.toString( cell.maxAsLongArray() );
			System.out.println( "Prediction started: " + cellLocation );
			final RandomAccessibleInterval< I > crop = Views.zeroMin( Views.interval( input, cell ) );
			// For testing: just copy the raw data over
			//RealTypeConverters.copyFromTo( crop, Views.zeroMin( cell ) );

			if( true )
			{
				// Convert data type (all models need float as input)
				//
				// TODO: Use converter to do this lazy
				//    also apply preprocessing ?

				RandomAccessibleInterval< FloatType > rai = Tensor.createCopyOfRaiInWantedDataType( (RandomAccessibleInterval) crop, new FloatType() );

				// Add channel and batch dimension
				//
				final long[] inputDims = rai.dimensionsAsLongArray();
				rai = Views.addDimension( rai, 0, 0 );
				rai = Views.moveAxis( rai, rai.numDimensions() - 1, 0 );
				rai = Views.addDimension( rai, 0, 0 );
				rai = Views.moveAxis( rai, rai.numDimensions() - 1, 0 );
				final long[] convertedDims = rai.dimensionsAsLongArray();


				// Preprocess?
				//
				// TODO

				// Build input tensor
				//
				Tensor< FloatType > inputTensor = Tensor.build( "input0", "bczyx", rai );
				List< Tensor< ? > > inputs = new ArrayList<>();
				inputs.add( inputTensor );

				// Prepare output tensor
				//
				// TODO
				//   - can we give the cell?
				final Tensor< O > outputTensor = Tensor.buildEmptyTensor( "output0", "bczyx" );
				List< Tensor< ? > > outputs = new ArrayList<>();
				outputs.add( outputTensor );

				try
				{
					outputs = model.runModel( inputs, outputs );
					RandomAccessibleInterval< FloatType > output = ( RandomAccessibleInterval< FloatType > ) outputs.get( 0 ).getData();
					// Remove batch and channel dimension
					output = Views.hyperSlice( output, 0, 0 );
					output = Views.hyperSlice( output, 0, 0 );

					// Copy output into cell
					//
					final long[] outputDims = output.dimensionsAsLongArray();
					final long[] cellDims = cell.dimensionsAsLongArray();

					RealTypeConverters.copyFromTo( Views.zeroMin( output ), Views.zeroMin(  cell ) );

					System.out.println("Prediction done: " + cellLocation );
					BdvFunctions.show( cell, cellLocation );
				} catch ( Exception e )
				{
					e.printStackTrace();
					throw new RuntimeException( e );
				}
			}
		}
	}
}
