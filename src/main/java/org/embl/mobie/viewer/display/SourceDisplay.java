package org.embl.mobie.viewer.display;

import org.embl.mobie.viewer.bdv.render.BlendingMode;

import java.util.List;

public interface SourceDisplay
{
	List< String > getSources();
	BlendingMode getBlendingMode();
	String getName();
	double getOpacity();
	boolean isVisible();
}
