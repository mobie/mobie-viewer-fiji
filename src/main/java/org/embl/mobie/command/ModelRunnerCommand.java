/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.command;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.util.RandomAccessibleIntervalMipmapSource;
import bdv.util.VolatileRandomAccessibleIntervalMipmapSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.VolatileFloatType;
import net.imglib2.view.Views;
import org.bioimageanalysis.icy.deeplearning.engine.EngineInfo;
import org.bioimageanalysis.icy.deeplearning.model.Model;
import org.bioimageanalysis.icy.deeplearning.predict.AxesMatcher;
import org.bioimageanalysis.icy.deeplearning.predict.ModelSpec;
import org.bioimageanalysis.icy.deeplearning.predict.PredictorOp;
import org.bioimageanalysis.icy.deeplearning.predict.ShapeMath;
import org.embl.mobie.lib.ThreadHelper;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Predict>Run Model on Current Source")
public class ModelRunnerCommand implements BdvPlaygroundActionCommand
{
	@Parameter
	public BdvHandle bdvHandle;

	@Parameter(label = "Resolution level")
	public int resolutionLevel = 3;

	@Parameter(label = "Model directory", style = "directory" )
	public File modelDirectory = new File( "/Users/tischer/Desktop/deep-models/platynereisemnucleisegmentationboundarymodel_torchscript" );

	@Parameter(label = "Engines directory", style = "directory" )
	public File enginesDirectory = new File( "/Users/tischer/Desktop/deep-engines" );

	@Parameter(label = "Engine")
	public String engine = "torchscript";;

	@Parameter(label = "Engine version")
	private String engineVersion = "1.9.1";

	@Override
	public void run()
	{
		//new Throwable().printStackTrace();

		final SourceAndConverter< ? > sourceAndConverter = bdvHandle.getViewerPanel().state().getCurrentSource();

		ModelSpec modelSpec = loadModelSpec( modelDirectory );

		final Model model = loadModel( modelDirectory, enginesDirectory );

		int timepoint = bdvHandle.getViewerPanel().state().getCurrentTimepoint();

		Source< ? > inputSource = sourceAndConverter.getSpimSource();

		final RandomAccessibleInterval< ? > inputImage = inputSource.getSource( timepoint, resolutionLevel );

		final RandomAccessibleInterval< FloatType > floatInputImage = RealTypeConverters.convert( ( RandomAccessibleInterval< ? extends RealType< ? > > ) inputImage, new FloatType() );

		List< RandomAccessibleInterval< FloatType > > xyzOutputs = createLazyXYZOutputImages( floatInputImage, model, modelSpec );

		for ( RandomAccessibleInterval< FloatType > xyzOutput : xyzOutputs )
		{
			final VolatileRandomAccessibleIntervalMipmapSource< FloatType, VolatileFloatType > predictionSource = wrapAsSource( xyzOutput, inputSource, timepoint );

			final BdvStackSource< ? > stackSource = BdvFunctions.show( predictionSource, BdvOptions.options().addTo( bdvHandle ) );
			stackSource.setDisplayRange( 0, 1 ); // TODO: fetch from model!
			stackSource.setColor( new ARGBType( ARGBType.rgba( 255, 0, 255, 255 ) ) );
		}
	}

	private ModelSpec loadModelSpec( File modelDirectory )
	{
		try
		{
			return ModelSpec.from( new File( modelDirectory, "rdf.yaml" ).getPath() );
		} catch ( FileNotFoundException e )
		{
			throw new RuntimeException( e );
		}
	}

	private VolatileRandomAccessibleIntervalMipmapSource< FloatType, VolatileFloatType > wrapAsSource( RandomAccessibleInterval< FloatType > prediction, Source< ? > inputSource, int timepoint )
	{
		final AffineTransform3D predictionTransform = new AffineTransform3D();
		inputSource.getSourceTransform( timepoint, resolutionLevel, predictionTransform  );

		final RandomAccessibleInterval< FloatType >[] predictions = new RandomAccessibleInterval[ 1 ];
		predictions[ 0 ] = prediction;

		// TODO: Apply scale factor to the prediction voxel dimensions?
		final double[] sourceVoxelSize = inputSource.getVoxelDimensions().dimensionsAsDoubleArray();
		final double[] predictionVoxelSize = new double[ resolutionLevel ];
		predictionTransform.apply( sourceVoxelSize, predictionVoxelSize );
		final FinalVoxelDimensions predictionVoxelDimensions = new FinalVoxelDimensions( "micrometer", predictionVoxelSize );

		final double[][] mipmapScales = new double[ 1 ][ resolutionLevel ];
		mipmapScales[ 0 ] = new double[]{ 1, 1, 1 };
		final RandomAccessibleIntervalMipmapSource< FloatType > predictionSource = new RandomAccessibleIntervalMipmapSource( predictions, new FloatType(), mipmapScales, predictionVoxelDimensions, predictionTransform, "prediction" );
		final VolatileRandomAccessibleIntervalMipmapSource< FloatType, VolatileFloatType > vPredictionSource = new VolatileRandomAccessibleIntervalMipmapSource( predictionSource, new VolatileFloatType(), ThreadHelper.sharedQueue );
		return vPredictionSource;
	}

	private List< RandomAccessibleInterval< FloatType > > createLazyXYZOutputImages( RandomAccessibleInterval< FloatType > xyzInput, Model model, ModelSpec modelSpec )
	{
		// create model input image
		final RandomAccessibleInterval< FloatType > modelInput = AxesMatcher.matchAxes( modelSpec.inputAxes, "xyz", xyzInput );

		// instantiate predictor with the input image
		final PredictorOp< FloatType, FloatType > predictorOp = new PredictorOp<>( model, Views.extendMirrorSingle( modelInput ), modelSpec );

		// create model output image
		//
		final String outputDataType = modelSpec.outputDataType;
		// TODO: Use the outputDataType
		final FloatType type = new FloatType();

		final ShapeMath shapeMath = new ShapeMath( modelSpec );

		// TODO remove the long mapping if we change it in the modelSpec
		final int[] outputCellDimensions =
				Arrays.stream(
						shapeMath.getOutputDimensions(
								Arrays.stream( modelSpec.inputShapeMin )
										.mapToLong( x -> x ).toArray() )
				).mapToInt( x -> ( int ) x ).toArray();

		final long[] outputInterval = shapeMath.getOutputDimensions( modelInput.dimensionsAsLongArray() );

		System.out.println("Output image dimensions: " + Arrays.toString( outputInterval ) );
		System.out.println("Output cell dimensions: " + Arrays.toString( outputCellDimensions ) );

		RandomAccessibleInterval< FloatType > modelOutput =
				new ReadOnlyCachedCellImgFactory().create(
						outputInterval,
						type,
						predictorOp::accept,
						ReadOnlyCachedCellImgOptions.options().cellDimensions( outputCellDimensions )
				);

		final RandomAccessibleInterval< FloatType > cxyzOutput = AxesMatcher.matchAxes( "cxyz", modelSpec.outputAxes, modelOutput );

		final ArrayList< RandomAccessibleInterval< FloatType > > xyzOutputs = new ArrayList<>();
		for ( int c = 0; c < cxyzOutput.dimension( 0 ); c++ )
		{
			xyzOutputs.add( Views.hyperSlice( cxyzOutput , 0, c ) );
		}

		return xyzOutputs;
	}

	// TODO: add to the model runner library
	private Model loadModel( File modelDirectory, File engineDirectory )
	{
		try
		{
			final String modelSource = new File( modelDirectory, "weights-torchscript.pt" ).getAbsolutePath();
			final boolean cpu = true;
			final boolean gpu = true;
			final String engineDirectoryAbsolutePath = engineDirectory.getAbsolutePath();
			final String modelDirectoryAbsolutePath = modelDirectory.getAbsolutePath();

			System.out.println( "Engine dir: " + engineDirectoryAbsolutePath);
			System.out.println( "Model dir: " + modelDirectoryAbsolutePath);
			System.out.println( "Model source: " + modelSource);

			final EngineInfo engineInfo = EngineInfo.defineDLEngine( engine, engineVersion, engineDirectoryAbsolutePath, cpu, gpu );
			final Model model = Model.createDeepLearningModel( modelDirectoryAbsolutePath, modelSource, engineInfo );

			model.loadModel();
			return model;
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}
}
