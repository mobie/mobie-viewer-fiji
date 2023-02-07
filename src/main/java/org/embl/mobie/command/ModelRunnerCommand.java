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
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.VolatileFloatType;
import org.embl.mobie.lib.ThreadHelper;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.io.File;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Predict>ModelZoo")
public class ModelRunnerCommand implements BdvPlaygroundActionCommand
{
	@Parameter
	public BdvHandle bdvHandle;

	@Parameter(label = "Source")
	public SourceAndConverter< ? > sourceAndConverter;

	@Parameter(label = "Model directory", style = "directory" )
	public File modelDirectory = new File( "/Users/tischer/Desktop/deep-models/platynereisemnucleisegmentationboundarymodel_torchscript" );

	@Parameter(label = "Engine directory", style = "directory" )
	public File engineDirectory = new File( "/Users/tischer/Desktop/deep-engines/Pytorch-1.9.1-1.9.1-macosx-x86_64-cpu-gpu" );

	private Source< ? > source;
	private int resolutionLevel;
	private int timepoint;

	@Override
	public void run()
	{
		timepoint = bdvHandle.getViewerPanel().state().getCurrentTimepoint();

		final RandomAccessibleInterval< ? > input = getInput( timepoint );

		RandomAccessibleInterval< FloatType > prediction = getPrediction( input );

		final VolatileRandomAccessibleIntervalMipmapSource< FloatType, VolatileFloatType > predictionSource = asSource( prediction );

		final BdvStackSource< ? > stackSource = BdvFunctions.show( predictionSource, BdvOptions.options().addTo( bdvHandle ) );
		stackSource.setDisplayRange( 0, 1 ); // TODO: fetch from model!
		stackSource.setColor( new ARGBType( ARGBType.rgba( 255, 0, 255, 255) ) );

	}

	private VolatileRandomAccessibleIntervalMipmapSource< FloatType, VolatileFloatType > asSource( RandomAccessibleInterval< FloatType > prediction )
	{
		final AffineTransform3D predictionTransform = new AffineTransform3D();
		source.getSourceTransform( timepoint, resolutionLevel, predictionTransform  );

		final RandomAccessibleInterval< FloatType >[] predictions = new RandomAccessibleInterval[ 1 ];
		predictions[ 0 ] = prediction;

		// TODO: Apply scale factor to the prediction voxel dimensions?
		final double[] sourceVoxelSize = source.getVoxelDimensions().dimensionsAsDoubleArray();
		final double[] predictionVoxelSize = new double[ resolutionLevel ];
		predictionTransform.apply( sourceVoxelSize, predictionVoxelSize );
		final FinalVoxelDimensions predictionVoxelDimensions = new FinalVoxelDimensions( "micrometer", predictionVoxelSize );

		final double[][] mipmapScales = new double[ 1 ][ resolutionLevel ];
		mipmapScales[ 0 ] = new double[]{ 1, 1, 1 };
		final RandomAccessibleIntervalMipmapSource< FloatType > predictionSource = new RandomAccessibleIntervalMipmapSource( predictions, new FloatType(), mipmapScales, predictionVoxelDimensions, predictionTransform, "prediction" );
		final VolatileRandomAccessibleIntervalMipmapSource< FloatType, VolatileFloatType > vPredictionSource = new VolatileRandomAccessibleIntervalMipmapSource( predictionSource, new VolatileFloatType(), ThreadHelper.sharedQueue );
		return vPredictionSource;
	}

	private RandomAccessibleInterval< FloatType > getPrediction( RandomAccessibleInterval< ? > input )
	{
		// TODO: determine from model
		final long[] dimensions = input.dimensionsAsLongArray();
		final FloatType type = new FloatType();
		final int[] cellDimensions = { 256, 256, 32 };

		RandomAccessibleInterval< FloatType > prediction =
				new ReadOnlyCachedCellImgFactory().create(
						dimensions,
						type,
						new CellLoader< FloatType >()
						{
							@Override
							public void load( SingleCellArrayImg< FloatType, ? > cell ) throws Exception
							{
								// TODO: Use Predictor-Op
							}
						},
						ReadOnlyCachedCellImgOptions.options().cellDimensions( cellDimensions )
				);

		return prediction;
	}

	private RandomAccessibleInterval< ? > getInput( int timepoint )
	{
		source = sourceAndConverter.getSpimSource();
		final int numMipmapLevels = source.getNumMipmapLevels();
		resolutionLevel = 3; // TODO: determine somehow
		final RandomAccessibleInterval< ? > inputRAI = source.getSource( timepoint, resolutionLevel );
		return inputRAI;
	}
}
