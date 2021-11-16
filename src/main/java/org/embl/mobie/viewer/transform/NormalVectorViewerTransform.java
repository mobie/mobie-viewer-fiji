package org.embl.mobie.viewer.transform;

import bdv.util.Affine3DHelpers;
import bdv.util.Bdv;
import de.embl.cba.bdv.utils.BdvUtils;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;

import java.util.stream.DoubleStream;

import static de.embl.cba.bdv.utils.BdvUtils.changeBdvViewerTransform;
import static de.embl.cba.bdv.utils.BdvUtils.getBdvWindowCenter;
import static de.embl.cba.bdv.utils.BdvUtils.quaternionToAffineTransform3D;

public class NormalVectorViewerTransform implements ViewerTransform
{
	// Serialisation
	private double[] normalVector; // required
	private final Integer timepoint; // optional

	public NormalVectorViewerTransform( double[] normalVector, int timepoint )
	{
		this.normalVector = normalVector;
		this.timepoint = timepoint;
	}

	@Override
	public double[] getParameters()
	{
		return normalVector;
	}

	@Override
	public Integer getTimepoint()
	{
		return timepoint;
	}

	public static AffineTransform3D createTransform( Bdv bdv, double[] targetNormalVector )
	{
		double[] currentNormalVector = BdvUtils.getCurrentViewNormalVector( bdv );

		AffineTransform3D currentViewerTransform = new AffineTransform3D();
		bdv.getBdvHandle().getViewerPanel().state().getViewerTransform( currentViewerTransform );

		LinAlgHelpers.normalize( targetNormalVector ); // just to be sure.

		// determine rotation axis
		double[] rotationAxis = new double[ 3 ];
		LinAlgHelpers.cross( currentNormalVector, targetNormalVector, rotationAxis );
		if ( LinAlgHelpers.length( rotationAxis ) > 0 ) LinAlgHelpers.normalize( rotationAxis );

		// The rotation axis is in the coordinate system of the original data set => transform to viewer coordinate system
		double[] qCurrentRotation = new double[ 4 ];
		Affine3DHelpers.extractRotation( currentViewerTransform, qCurrentRotation );
		final AffineTransform3D currentRotation = quaternionToAffineTransform3D( qCurrentRotation );

		double[] rotationAxisInViewerSystem = new double[ 3 ];
		currentRotation.apply( rotationAxis, rotationAxisInViewerSystem );

		// determine rotation angle
		double angle = - Math.acos( LinAlgHelpers.dot( currentNormalVector, targetNormalVector ) );

		// construct rotation of angle around axis
		double[] rotationQuaternion = new double[ 4 ];
		LinAlgHelpers.quaternionFromAngleAxis( rotationAxisInViewerSystem, angle, rotationQuaternion );
		final AffineTransform3D rotation = quaternionToAffineTransform3D( rotationQuaternion );

		// apply transformation (rotating around current viewer centre position)
		final AffineTransform3D translateCenterToOrigin = new AffineTransform3D();
		translateCenterToOrigin.translate( DoubleStream.of( getBdvWindowCenter( bdv )).map( x -> -x ).toArray() );

		final AffineTransform3D translateCenterBack = new AffineTransform3D();
		translateCenterBack.translate( getBdvWindowCenter( bdv ) );

		final AffineTransform3D affineTransform3D = currentViewerTransform.copy()
				.preConcatenate( translateCenterToOrigin )
				.preConcatenate( rotation )
				.preConcatenate( translateCenterBack );

		return affineTransform3D;
	}
}
