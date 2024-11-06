package org.embl.mobie.lib.bvv;

import btbvv.vistools.Bvv;
import btbvv.vistools.BvvFunctions;

public class BVVManager
{
	private Bvv bvv;
	//BVV rendering parameters, can be changed/adjusted somewhere else
	double dCam = 2000.;
	
	double dClipNear = 1000.;
	double dClipFar = 15000.;			
	int renderWidth = 800;
	int renderHeight = 600;
	int numDitherSamples = 3; 
	int cacheBlockSize = 32;
	int  maxCacheSizeInMB = 500;
	int ditherWidth = 3;
	
	public synchronized Bvv get()
	{
		if ( bvv == null )
		{
			bvv = BvvFunctions.show( Bvv.options().frameTitle( "BigVolumeViewer" ).
					dCam(dCam).
					dClipNear(dClipNear).
					dClipFar(dClipFar).				
					renderWidth(renderWidth).
					renderHeight(renderHeight).
					numDitherSamples(numDitherSamples ).
					cacheBlockSize(cacheBlockSize ).
					maxCacheSizeInMB( maxCacheSizeInMB ).
					ditherWidth(ditherWidth)
					);
		}
		return bvv;
	}

	public void setBVV( Bvv bvv )
	{
		this.bvv = bvv;
	}

	public void close()
	{
		if ( bvv != null )
		{
			bvv.close();
		}
	}
}
