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
import net.imglib2.type.numeric.ARGBType;
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
		final int predictionResolutionLevel = 3;
		final SharedQueue sharedQueue = new SharedQueue( 3 );

		// Open the input data
		//
		final AbstractSpimData platySBEM = new SpimDataOpener().openSpimData( "https://s3.embl.de/i2k-2020/platy-raw.ome.zarr", ImageDataFormat.OmeZarrS3, sharedQueue );

		// Show the input data in BDV
		final List< BdvStackSource< ? > > stackSources = BdvFunctions.show( platySBEM );
		// Get Source containing EM data
		final BdvStackSource< ? > emStackSource = stackSources.get( 0 );
		final Source< ? > emSource = emStackSource.getSources().get( 0 ).getSpimSource();

		// Get a bdv handle
		final BdvHandle bdvHandle = emStackSource.getBdvHandle();

		// Make the cell segmentation invisible
		// (the ome.zarr contains two images, the EM raw data
		// and a cell segmentation label mask)
		bdvHandle.getViewerPanel().state().setSourceActive( stackSources.get( 1 ).getSources().get( 0 ), false );

		// Focus on some smaller area within the volume
		final AffineTransform3D viewerTransform = new AffineTransform3D();
		viewerTransform.set( 31.524161974149372,0.0,0.0,-3471.2941398257967,0.0,31.524161974149372,0.0,-3335.2908913145466,0.0,0.0,31.524161974149372,-4567.901470761989 );
		bdvHandle.getViewerPanel().state().setViewerTransform( viewerTransform );
		IJ.wait( 1000 );

		// Create the lazy prediction image
		//
		final RandomAccessibleInterval< ? > inputRAI = emSource.getSource( 0, predictionResolutionLevel );
		// TODO add padding!
		RandomAccessibleInterval< FloatType > predictionRAI =
				new ReadOnlyCachedCellImgFactory().create(
						inputRAI.dimensionsAsLongArray(),
						new FloatType(),
						new Predictor( loadModel(), inputRAI ),
						ReadOnlyCachedCellImgOptions.options().cellDimensions( 256, 256, 32 )
				);


		// Map the prediction image into the same coordinate system
		// as the input image
		//
		// For this we wrap the predictionRAI into a Source
		//
		final AffineTransform3D predictionTransform = new AffineTransform3D();
		emSource.getSourceTransform( 0, predictionResolutionLevel, predictionTransform  );

		final RandomAccessibleInterval< FloatType >[] predictionRais = new RandomAccessibleInterval[ 1 ];
		predictionRais[ 0 ] = predictionRAI;

		// TODO: Fix the voxel dimensions
		final double[] sourceVoxelSize = emSource.getVoxelDimensions().dimensionsAsDoubleArray();
		final double[] predictionVoxelSize = new double[ predictionResolutionLevel ];
		predictionTransform.apply( sourceVoxelSize, predictionVoxelSize );
		final FinalVoxelDimensions predictionVoxelDimensions = new FinalVoxelDimensions( "micrometer", predictionVoxelSize );

		final double[][] mipmapScales = new double[ 1 ][ predictionResolutionLevel ];
		mipmapScales[ 0 ] = new double[]{ 1, 1, 1 };
		final RandomAccessibleIntervalMipmapSource predictionSource = new RandomAccessibleIntervalMipmapSource( predictionRais, new FloatType(), mipmapScales, predictionVoxelDimensions, predictionTransform, "prediction" );
		final VolatileRandomAccessibleIntervalMipmapSource vPredictionSource = new VolatileRandomAccessibleIntervalMipmapSource( predictionSource, new VolatileFloatType(), sharedQueue );

		// Show the prediction Source
		//
		final BdvStackSource bdvStackSource = BdvFunctions.show( vPredictionSource, BdvOptions.options().addTo( bdvHandle ) );
		bdvStackSource.setDisplayRange( 0, 1 );
		bdvStackSource.setColor( new ARGBType( ARGBType.rgba( 255, 0, 255, 255) ) );
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

	static class Predictor< I extends RealType< I > & NativeType< I >, O extends RealType< O > & NativeType< O > >  implements CellLoader< O >
	{
		private final Model model;
		private final RandomAccessibleInterval< I > input;
		private I inputType;

		public Predictor( Model model, RandomAccessibleInterval< I > input )
		{
			this.model = model;
			this.input = input;
			inputType = Util.getTypeFromInterval( input );

			//final Converter< I, FloatType > converter = RealTypeConverters.getConverter( inputType, new FloatType() );
		}

		@Override
		public void load( SingleCellArrayImg< O, ? > cell )
		{
			final String cellLocation = Arrays.toString( cell.minAsLongArray() ) + " - " + Arrays.toString( cell.maxAsLongArray() );
			System.out.println( "Prediction started: " + cellLocation );
			final RandomAccessibleInterval< I > crop = Views.zeroMin( Views.interval( input, cell ) );
			// For testing: copy the raw data over
			//RealTypeConverters.copyFromTo( crop, Views.zeroMin( cell ) );

			if( true )
			{
				// Convert data type (all models need float as input)
				//
				// TODO: use converter to do this lazy
				// TODO: also apply preprocessing ?

				final O outputType = Util.getTypeFromInterval( cell );
				RandomAccessibleInterval< O > rai = Tensor.createCopyOfRaiInWantedDataType( (RandomAccessibleInterval) crop, outputType );

				// Add channel and batch dimension
				//

				// TODO: Padding and intialize in Constructor
				final long[] inputDims = rai.dimensionsAsLongArray();
				rai = Views.addDimension( rai, 0, 0 );
				rai = Views.moveAxis( rai, rai.numDimensions() - 1, 0 );
				rai = Views.addDimension( rai, 0, 0 );
				rai = Views.moveAxis( rai, rai.numDimensions() - 1, 0 );
				final long[] convertedDims = rai.dimensionsAsLongArray();

				// Preprocess?
				//
				// TODO

				// There are 4 types!


				// ModelParser
				// output: padding, input and output type, axis order
				//
				// use getters from ModelParser to build PredictionOp
				// PredictionOp should have the
				// correct data types just for the model!

				//


				// Build input tensor
				//
				// What is the type for the input tensor?
				Tensor< ? > inputTensor = Tensor.build( "input0", "bczyx", rai );
				List< Tensor< ? > > inputs = new ArrayList<>();
				inputs.add( inputTensor );

				// Prepare output tensor
				//
				// TODO: could we instead give the cell?
				// What is the type for the output tensor?
				final Tensor< O > outputTensor = Tensor.buildEmptyTensor( "output0", "bczyx" );
				List< Tensor< O > > outputs = new ArrayList<>();
				outputs.add( outputTensor );

				try
				{
					outputs = model.runModel( inputs, outputs );
					RandomAccessibleInterval< O > output = ( RandomAccessibleInterval< O > ) outputs.get( 0 ).getData();
					// Remove batch and channel dimension
					output = Views.hyperSlice( output, 0, 0 );
					output = Views.hyperSlice( output, 0, 0 );

					// Copy output into cell
					//
					final long[] outputDims = output.dimensionsAsLongArray();
					final long[] cellDims = cell.dimensionsAsLongArray();

					RealTypeConverters.copyFromTo( Views.zeroMin( output ), Views.zeroMin(  cell ) );

					System.out.println("Prediction done: " + cellLocation );
					//BdvFunctions.show( cell, cellLocation );
				} catch ( Exception e )
				{
					e.printStackTrace();
					throw new RuntimeException( e );
				}
			}
		}
	}
}
