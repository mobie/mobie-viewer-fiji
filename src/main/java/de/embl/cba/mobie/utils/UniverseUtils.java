package de.embl.cba.mobie.utils;

import ij3d.Image3DUniverse;
import org.scijava.java3d.Transform3D;

import java.util.HashMap;

public abstract class UniverseUtils
{
	// TODO: refactor to UniverseUtils
	public static void setVolumeView( HashMap< String, String > transforms, Image3DUniverse universe )
	{
		String tmp;
		// Set up new Content
		if ((tmp = transforms.get("center")) != null) universe.getCenterTG().setTransform(
				asTransform(tmp));
		if ((tmp = transforms.get("translate")) != null) universe.getTranslateTG()
				.setTransform( asTransform(tmp));
		if ((tmp = transforms.get("rotate")) != null) universe.getRotationTG().setTransform(
				asTransform(tmp));
		if ((tmp = transforms.get("zoom")) != null) universe.getZoomTG()
				.setTransform( asTransform(tmp));
		if ((tmp = transforms.get("animate")) != null) universe.getAnimationTG()
				.setTransform( asTransform(tmp));

		universe.getViewPlatformTransformer().updateFrontBackClip();
	}

	// TODO: refactor to UniverseUtils
	private static final Transform3D asTransform( final String s) {
		final String[] sp = s.split(" ");
		final float[] f = new float[16];
		for (int i = 0; i < sp.length; i++)
			f[i] = asFloat(sp[i]);
		return new Transform3D(f);
	}

	// TODO: refactor to UniverseUtils
	private static final float asFloat( final String s) {
		return Float.parseFloat(s);
	}

	public static final String toString( final Transform3D t3d ) {
		final float[] xf = new float[16];
		t3d.get(xf);
		String ret = "";
		for (int i = 0; i < 16; i++)
			ret += " " + xf[i];
		return ret;
	}

	public static String getVolumeViewerTransform( Image3DUniverse universe )
	{
		if ( universe == null ) return "";

		String transform = "";
		final Transform3D t3d = new Transform3D();
		universe.getCenterTG().getTransform(t3d);
		transform += "center = " + toString(t3d) +"\n";
		universe.getTranslateTG().getTransform(t3d);
		transform += "translate = " + toString(t3d) +"\n";
		universe.getRotationTG().getTransform(t3d);
		transform += "rotate = " + toString(t3d) +"\n";
		universe.getZoomTG().getTransform(t3d);
		transform += "zoom = " + toString(t3d) +"\n";
		universe.getAnimationTG().getTransform(t3d);
		transform += "animate = " + toString(t3d) +"\n";

		return transform;
	}

	// TODO: refactor to UniverseUtils
	public static void setVolumeView( String view, Image3DUniverse universe )
	{
		UniverseUtils.setVolumeView( getTransforms( view ), universe );
	}

	// TODO: refactor to UniverseUtils
	private static HashMap< String, String > getTransforms( String view )
	{
		final HashMap<String, String> transforms = new HashMap<String, String>();

		final String[] lines = view.split( "\n" );

		for ( String line : lines )
		{
			final String[] keyval = line.split("=");
			transforms.put(keyval[0].trim(), keyval[1].trim());
		}
		return transforms;
	}
}
