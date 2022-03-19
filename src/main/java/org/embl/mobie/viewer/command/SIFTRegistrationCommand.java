package org.embl.mobie.viewer.command;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import mpicbg.models.AffineModel2D;
import mpicbg.models.InterpolatedAffineModel2D;
import mpicbg.models.RigidModel2D;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.type.numeric.real.FloatType;
import org.embl.mobie.viewer.bdv.SourceViewRasterizer;
import org.janelia.saalfeldlab.hotknife.MultiConsensusFilter;
import org.janelia.saalfeldlab.hotknife.util.Align;
import org.janelia.saalfeldlab.hotknife.util.Transform;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.io.Serializable;
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
		final RandomAccessibleInterval< FloatType > fixedRai = new SourceViewRasterizer( bdvHandle, fixedSource.getSpimSource() ).getRasterizedSourceView();

		final RandomAccessibleInterval< FloatType > movingRai = new SourceViewRasterizer( bdvHandle, movingSource.getSpimSource() ).getRasterizedSourceView();

		final Transform.InterpolatedAffineModel2DSupplier<AffineModel2D, RigidModel2D> filterModelSupplier =
				new Transform.InterpolatedAffineModel2DSupplier<AffineModel2D, RigidModel2D>( ( Supplier<AffineModel2D> & Serializable )AffineModel2D::new, (Supplier<RigidModel2D> & Serializable)RigidModel2D::new, 0.25 );

		final MultiConsensusFilter< InterpolatedAffineModel2D< AffineModel2D, RigidModel2D > > filter = new MultiConsensusFilter<InterpolatedAffineModel2D<AffineModel2D, RigidModel2D>>(
				filterModelSupplier,
				10000,
				3,
				0.0,
				7);

		final AffineTransform2D affineTransform2D = Align.alignSIFT(
				fixedRai,
				movingRai,
				1.0,
				0.75,
				4,
				0.92,
				1.0,
				filter,
				filterModelSupplier,
				Transform::convertAndInvertAffine2DtoAffineTransform2D );

		System.out.println( affineTransform2D );

	}

}
