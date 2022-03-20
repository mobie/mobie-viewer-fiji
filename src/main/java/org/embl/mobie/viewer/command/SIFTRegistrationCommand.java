package org.embl.mobie.viewer.command;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import mpicbg.models.AffineModel2D;
import mpicbg.models.InterpolatedAffineModel2D;
import mpicbg.models.RigidModel2D;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.real.FloatType;
import org.embl.mobie.viewer.bdv.SourceViewRasterizer;
import org.embl.mobie.viewer.transform.TransformHelper;
import org.janelia.saalfeldlab.hotknife.MultiConsensusFilter;
import org.janelia.saalfeldlab.hotknife.util.Align;
import org.janelia.saalfeldlab.hotknife.util.Transform;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Transform>Registration - SIFT")
public class SIFTRegistrationCommand implements BdvPlaygroundActionCommand
{
	@Parameter
	BdvHandle bdvHandle;

	@Parameter(label = "Fixed Source")
	SourceAndConverter fixedSource;

	@Parameter(label = "Moving Source")
	SourceAndConverter movingSource;

	@Override
	public void run()
	{
		final ArrayList< Source< ? > > sources = new ArrayList<>();
		sources.add( fixedSource.getSpimSource() );
		sources.add( movingSource.getSpimSource() );


		final SourceViewRasterizer rasterizer = new SourceViewRasterizer( bdvHandle, sources );
		final List< RandomAccessibleInterval< FloatType > > rasterRais = rasterizer.getRasterRais();

		IJ.log( "SIFT registration sampling: " + rasterizer.getRasterVoxelSize() + " " + sources.get( 0 ).getVoxelDimensions().unit() );

		//ImageJFunctions.show( rasterRais.get( 0 ), "fixedRai" );
		//ImageJFunctions.show( rasterRais.get( 1 ), "movingRai" );

		final Transform.InterpolatedAffineModel2DSupplier<AffineModel2D, RigidModel2D> filterModelSupplier = new Transform.InterpolatedAffineModel2DSupplier<AffineModel2D, RigidModel2D>( ( Supplier<AffineModel2D> & Serializable )AffineModel2D::new, (Supplier<RigidModel2D> & Serializable)RigidModel2D::new, 0.25 );

		final MultiConsensusFilter< InterpolatedAffineModel2D< AffineModel2D, RigidModel2D > > filter = new MultiConsensusFilter<InterpolatedAffineModel2D<AffineModel2D, RigidModel2D>>(
				filterModelSupplier,
				1000,
				3,
				0.0,
				7);

		final AffineTransform2D affineSIFT2D = Align.alignSIFT(
				rasterRais.get( 0 ),
				rasterRais.get( 1 ),
				1.0,
				0.75,
				4,
				0.92,
				1.0,
				filter,
				filterModelSupplier,
				Transform::convertAndInvertAffine2DtoAffineTransform2D );

		if ( affineSIFT2D == null )
		{
			IJ.log("SIFT registration failed.");
			return;
		}
		else
		{
			IJ.log("SIFT transform: " +  affineSIFT2D );
			transformMovingSource( affineSIFT2D, rasterizer.getRasterTransform() );
		}
	}

	private void transformMovingSource( AffineTransform2D affineSIFT2D, AffineTransform3D rasterTransform )
	{
		// The SIFT transform has been computed in the "rasterTransform" (RT)
		// coordinate space. Thus, the SIFT transform in real space
		// needs to be applied as (reading from right to left):
		// RT^(-1) SIFT RT
		final AffineTransform3D transform = rasterTransform.copy();
		final AffineTransform3D affineSIFT3D = TransformHelper.asAffineTransform3D( affineSIFT2D );
		transform.preConcatenate( affineSIFT3D );
		transform.preConcatenate( rasterTransform.copy().inverse() );

		// Append the transform to whatever transforms were there already
		final TransformedSource< ? > transformedSource = ( TransformedSource< ? > ) movingSource.getSpimSource();
		final AffineTransform3D fixedTransform = new AffineTransform3D();
		transformedSource.getFixedTransform( fixedTransform );
		fixedTransform.preConcatenate( transform );
		transformedSource.setFixedTransform( fixedTransform );
		bdvHandle.getViewerPanel().requestRepaint();
	}

}
