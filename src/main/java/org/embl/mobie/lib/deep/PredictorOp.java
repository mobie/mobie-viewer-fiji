package org.embl.mobie.lib.deep;

import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.bioimageanalysis.icy.deeplearning.model.Model;

import java.util.function.Consumer;


//
public class PredictorOp< I extends RealType< I > & NativeType< I >, O extends RealType< O > & NativeType< O > >
{
	private Model model;

	// Mimiching https://github.com/saalfeldlab/i2k2020-imglib2-advanced/blob/main/src/main/java/org/janelia/saalfeldlab/i2k2020/ops/CLIJ2FilterOp.java#L69
	public PredictorOp( final RandomAccessible< I > source )
	{
		//Tensor.build();
		model = Model.createDeepLearningModel();
		//model.runModel(  );
	}

	public Interval getOutputInterval( Interval inputInterval )
	{
		// the
		return outputInterval;
	}

	public void predict( Interval inputInterval, RandomAccessibleInterval< O > outputCell )
	{
		Interval outputInterval = outputInterval(model, inputInterval);

		// build inputTensors


		// add dimensions if needed

		final long[] inputDims = rai.dimensionsAsLongArray();
		rai = Views.addDimension( rai, 0, 0 );
		rai = Views.moveAxis( rai, rai.numDimensions() - 1, 0 );
		rai = Views.addDimension( rai, 0, 0 );
		rai = Views.moveAxis( rai, rai.numDimensions() - 1, 0 );
		final long[] convertedDims = rai.dimensionsAsLongArray();

		// build outputTensors


		model.runModel(  );

		// remove dimensions if needed


		// copy results into cell

	}
}
