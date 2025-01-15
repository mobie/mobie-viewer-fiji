package org.embl.mobie.lib.volume;

/*-
 * #%L
 * Various Java code for ImageJ
 * %%
 * Copyright (C) 2018 - 2024 EMBL
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

import ij3d.Content;
import ij3d.Image3DUniverse;
import org.jogamp.java3d.Canvas3D;
import org.jogamp.java3d.Transform3D;
import org.jogamp.vecmath.Point3d;
import org.jogamp.vecmath.Vector3d;

/*
 * The idea of the adjuster is to translate the eye such that all added points
 * are within the field of view.
 *
 * To this end, the field of view is modeled as an infinite pyramid with the
 * eye as tip and the four sides cutting through the borders of the window
 * (i.e. the canvas onto which everything is projected).
 *
 * The optimal such pyramid is the one which wraps all the points just so.  To
 * achieve this, the tip is set to the first point, and then the tip is only
 * translated when necessary, along the side of the pyramid that is _opposite_
 * of the point which is not yet inside, an the tip is only moved until the
 * closest side touches the point.
 *
 * The problem can be actually split into translations in the xz and the yz
 * plane, respectively, in which case we only have to take care of projections,
 * i.e. the points are still points, but the pyramid is actually a triangle.
 *
 * For simplicity, we just calculate the factor by which the tip would have
 * to move for _every_ side, and only move if that factor is positive.
 *
 * Example: assume that the projection P of a point into the xz plane is to
 * the right of the triangle (which is the projected pyramid) defined by the
 * eye (x, z), the distance (0, -e) of the eye to the canvas, and the canvas
 * width w.
 *
 * Then the eye has to be moved along the left side of the triangle to
 * (x', z') = (x, z) + m * (w/2, e) such that P lies on the new right side,
 * i.e. (P - (x', z')) is parallel to (-w/2, e), or in other words, the
 * scalar product between (P - (x', z')) and (e, w/2) is 0.
 *
 * It turns out that m = (P - (x, z)) (1 / w, -1 / 2e) fulfills that
 * requirement.
 *
 * If m <= 0, then the point P was not right of the triangle to begin with,
 * and no adjustment is necessary (actually, the eye must not be moved in
 * that case, because otherwise some other point would no longer be inside
 * the triangle afterwards, as the triangle was chosen such that it just so
 * fits the previous points).
 *
 * Substituting -w for w gives handles the case when the point was to the
 * left of the triangle.  Since the vectors for the right and left side have
 * the exact same length, we only need to consider only the larger m between
 * the right and left one.
 *
 * See HorizontalAdjuster for the implementation.
 */
public class AnimatedViewAdjuster
{

	/** Fit the points horizontally */
	public static final int ADJUST_HORIZONTAL = 0;

	/** Fit the points vertically */
	public static final int ADJUST_VERTICAL = 1;

	/** Fit the points both horizontally and vertically */
	public static final int ADJUST_BOTH = 2;

	private final Canvas3D canvas;
	private final Image3DUniverse univ;

	private final Point3d eye = new Point3d();
	private final Point3d oldEye = new Point3d();

	private final Transform3D toCamera = new Transform3D();
	private final Transform3D toCameraInverse = new Transform3D();
	private final int adjustmentDirection;

	private boolean firstPoint = true;

	private double e = 1.0;
	private double w = 2 * Math.tan(Math.PI / 8);
	private double h = 2 * Math.tan(Math.PI / 8);

	private Adjuster adjuster;

	private interface Adjuster {
		void add( Point3d p );
	}

	/**
	 * Create a new ViewAdjuster.
	 *
	 * @param adjustmentDirection One of ADJUST_HEIGHT, ADJUST_WIDTH or ADJUST_BOTH. The former
	 *          adjusts the view so that the added points fit in width, the latter
	 *          so that they fit in height in the canvas.
	 */
	public AnimatedViewAdjuster( final Image3DUniverse univ, final int adjustmentDirection) {
		this.univ = univ;
		this.canvas = univ.getCanvas();
		this.adjustmentDirection = adjustmentDirection;
	}

	private void updateViewAdjustments()
	{
		firstPoint = true;

		switch (adjustmentDirection) {
			case ADJUST_HORIZONTAL:
				adjuster = new HorizontalAdjuster();
				break;
			case ADJUST_VERTICAL:
				adjuster = new VerticalAdjuster();
				break;
			case ADJUST_BOTH:
				adjuster = new BothAdjuster();
				break;
			default:
				throw new IllegalArgumentException();
		}

		// get eye in image plate
		canvas.getCenterEyeInImagePlate(eye);
		// transform eye to vworld
		canvas.getImagePlateToVworld(toCamera);
		toCamera.transform(eye);
		// transform eye to camera
		univ.getVworldToCamera(toCamera);
		toCamera.transform(eye);

		// save the old eye pos
		oldEye.set(eye);

		final Transform3D toIpInverse = new Transform3D();
		canvas.getImagePlateToVworld(toIpInverse);

		// get the upper left canvas corner in camera coordinates
		final Point3d lu = new Point3d();
		canvas.getPixelLocationInImagePlate(0, 0, lu);
		toIpInverse.transform(lu);
		toCamera.transform(lu);

		// get the lower right canvas corner in camera coordinates
		final Point3d rl = new Point3d();
		canvas.getPixelLocationInImagePlate(canvas.getWidth(), canvas.getHeight(),
				rl);
		toIpInverse.transform(rl);
		toCamera.transform(rl);

		w = rl.x - lu.x;
		h = rl.y - lu.y;

		e = -rl.z;

		univ.getVworldToCameraInverse(toCameraInverse);
	}

	public void apply( Content content, int fps, int durationMillis )
	{
		apply( content, fps, durationMillis, 1.0, 0, 0 );
	}


	public void apply( Content content, int fps, int durationMillis, double zoomLevel )
	{
		apply( content, fps, durationMillis, zoomLevel, 0, 0 );
	}

	/**
	 * After all points/contents were added, apply() finally adjusts center and
	 * zoom transformations of the view.
	 *
	 * The adjustment is animated.
	 * Currently, when a new object is added, it is zoomed in on it from an overview perspective.
	 * @param content
	 * @param fps
	 * 				The frequency of the animation in frames per seconds (Hz). The animation
	 * 			frequency of movies in the cinema is around 30 Hz.
	 * @param durationMillis
	 * @param zoomLevel
	 * @param dxyMin
	 * @param dzMin
	 */
	public void apply(
			Content content,
			int fps,
			int durationMillis,
			double zoomLevel,
			double dxyMin,
			double dzMin ) {
		/* The camera to vworld transformation is given by
		 * C * T * R * Z, where
		 *
		 * C: center transformation
		 * T: user defined translation
		 * R: rotation
		 * Z: zoom translation
		 *
		 * the calculated eye coordinate can be thought of as
		 * an additional transformation A, so that we have
		 * C * T * R * Z * A.
		 *
		 * A should be split into A_z, the z translation, which
		 * is incorporated into the Zoom translation Z, and
		 * A_xy, which should be incorporated into the center
		 * transformation C.
		 *
		 * C * T * R * A * Z_new = C_new * T * R * Z_new
		 *
		 * where Z_new = A_z * Z.
		 *
		 * C_new is then C * T * R * A_xy * R^-1 * T^-1
		 */
		final Transform3D t3d = new Transform3D();
		final Vector3d transl = new Vector3d();

		final int numFrames = (int) ( durationMillis / 1000.0 * fps );
		final int intervalBetweenFramesMillis = ( int ) ( 1.0 * 1000.0 / fps );

		//eye.z += 1.0 / zoomLevel;

		this.updateViewAdjustments( );
		this.add( content );

		double dx = eye.x - oldEye.x;
		double dy = eye.y - oldEye.y;
		double dz = eye.z - oldEye.z;

		if ( Math.abs( dx ) < dxyMin  && Math.abs( dy ) < dxyMin && Math.abs( dz ) < dzMin  )
			return;

		for ( int i = 1; i <= numFrames; i++ )
		{
			this.updateViewAdjustments( );
			this.add( content );

			dx = eye.x - oldEye.x;
			dy = eye.y - oldEye.y;
			dz = eye.z + 1.0 / zoomLevel - oldEye.z;

			final double ddx = dx * i / numFrames;
			final double ddy = dy * i / numFrames;
			final double ddz = dz * i / numFrames;

			// adjust zoom
			univ.getZoomTG().getTransform( t3d );
			t3d.get( transl );
			transl.z += ddz;
			t3d.set( transl );
			univ.getZoomTG().setTransform( t3d );

			// adjust center
			final Transform3D tmp = new Transform3D();
			univ.getCenterTG().getTransform( t3d );
			univ.getTranslateTG().getTransform( tmp );
			t3d.mul( tmp );
			univ.getRotationTG().getTransform( tmp );
			t3d.mul( tmp );
			transl.set( ddx, ddy, 0d );
			tmp.set( transl );
			t3d.mul( tmp );
			univ.getRotationTG().getTransform( tmp );
			t3d.mulInverse( tmp );
			univ.getTranslateTG().getTransform( tmp );
			t3d.mulInverse( tmp );
			univ.getCenterTG().setTransform( t3d );

			sleep( intervalBetweenFramesMillis );
		}

		univ.getViewPlatformTransformer().updateFrontBackClip();

	}

	public void sleep( int millis )
	{
		try
		{
			Thread.sleep( millis );
		} catch ( InterruptedException ex )
		{
			ex.printStackTrace();
		}
	}

	/**
	 * Make sure that the average center of all contents is visible in the canvas.
	 */
	public void addCenterOf(final Iterable< Content > contents) {
		final Point3d center = new Point3d();
		final Point3d tmp = new Point3d();
		int counter = 0;
		for (final Content c : contents) {
			final Transform3D localToVworld = new Transform3D();
			c.getContent().getLocalToVworld(localToVworld);
			c.getContent().getMin(tmp);
			center.add(tmp);
			c.getContent().getMax(tmp);
			center.add(tmp);
			counter += 2;
		}
		center.x /= counter;
		center.y /= counter;
		center.z /= counter;
		add(center);
	}

	/**
	 * Add another Content which should be completely visible in the canvas.
	 */
	public void add(final Content c) {
		final Transform3D localToVworld = new Transform3D();
		c.getContent().getLocalToVworld(localToVworld);

		final Point3d min = new Point3d();
		c.getContent().getMin(min);

		final Point3d max = new Point3d();
		c.getContent().getMax(max);

		// transform each of the 8 corners to vworld
		// coordinates and feed it to add(Point3d).
		add(localToVworld, new Point3d(min.x, min.y, min.z));
		add(localToVworld, new Point3d(max.x, min.y, min.z));
		add(localToVworld, new Point3d(min.x, max.y, min.z));
		add(localToVworld, new Point3d(max.x, max.y, min.z));
		add(localToVworld, new Point3d(min.x, min.y, max.z));
		add(localToVworld, new Point3d(max.x, min.y, max.z));
		add(localToVworld, new Point3d(min.x, max.y, max.z));
		add(localToVworld, new Point3d(max.x, max.y, max.z));
	}

	/**
	 * Add another point which should be visible in the canvas; the point is
	 * expected to be in local coordinates, with the given local-to-vworld
	 * transformation.
	 */
	public void add(final Transform3D localToVworld, final Point3d local) {
		localToVworld.transform(local);
		add(local);
	}

	/**
	 * Add another point which should be visible in the canvas; the point is
	 * expected to be in vworld coordinates.
	 */
	public void add(final Point3d p) {
		adjuster.add(p);
	}

	private final class BothAdjuster implements Adjuster
	{

		private final Adjuster hAdj, vAdj;

		public BothAdjuster() {
			hAdj = new HorizontalAdjuster();
			vAdj = new VerticalAdjuster();
		}

		@Override
		public void add(final Point3d point) {
			final Point3d p = new Point3d(point);
			toCamera.transform(p);

			if (firstPoint) {
				eye.set(p.x, p.y, p.z);
				firstPoint = false;
				return;
			}
			vAdj.add(point);
			hAdj.add(point);
		}
	}

	private final class HorizontalAdjuster implements Adjuster
	{

		@Override
		public void add(final Point3d point) {
			final Point3d p = new Point3d(point);
			toCamera.transform(p);

			if (firstPoint) {
				eye.set(p.x, eye.y, p.z);
				firstPoint = false;
				return;
			}

			final double s1 = (p.x - eye.x) / w;
			final double s2 = (eye.z - p.z) / (2 * e);

			final double m1 = s1 - s2;
			final double m2 = -s1 - s2;

			if (m1 > m2) {
				if (m1 > 0) {
					eye.x += m1 * w / 2;
					eye.z += m1 * e;
				}
			}
			else {
				if (m2 > 0) {
					eye.x -= m2 * w / 2;
					eye.z += m2 * e;
				}
			}
		}
	}

	private final class VerticalAdjuster implements Adjuster
	{

		@Override
		public void add(final Point3d point) {
			final Point3d p = new Point3d(point);
			toCamera.transform(p);

			if (firstPoint) {
				eye.set(eye.x, p.y, p.z);
				firstPoint = false;
				return;
			}

			final double s1 = (p.y - eye.y) / h;
			final double s2 = (eye.z - p.z) / (2 * e);

			final double m1 = s1 - s2;
			final double m2 = -s1 - s2;

			if (m1 > m2) {
				if (m1 > 0) {
					eye.y += m1 * h / 2;
					eye.z += m1 * e;
				}
			}
			else {
				if (m2 > 0) {
					eye.y -= m2 * h / 2;
					eye.z += m2 * e;
				}
			}
		}
	}
}
