package org.embl.mobie.lib.bvb;

import org.embl.mobie.lib.serialize.display.VisibilityListener;

import bdv.viewer.SourceAndConverter;

public class BVBVisibilityListener implements VisibilityListener
{

	final SourceAndConverter< ? > sourceAndConverter;
	
	public BVBVisibilityListener(final SourceAndConverter< ? > sourceAndConverter_)
	{
		sourceAndConverter = sourceAndConverter_;
	}
	
	@Override
	public void visibility( boolean isVisible )
	{	
		
	}
	
	public SourceAndConverter< ? > getSAC()
	{
		return sourceAndConverter;
	}

}
