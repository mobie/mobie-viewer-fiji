package org.embl.mobie.lib.bvv;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import net.imglib2.FinalDimensions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.viewer.Source;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;

public class SourceToSpimDataWrapper
{
	/** wraps UnsignedByte, UnsignedShort, UnsignedLong or Float type source to a cached spimdata 
	 * (of UnsignedShort type) to display in BVV, otherwise returns null **/
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static AbstractSpimData< ? > wrap( final Source< ? > source )
	{
		final SourceToViewerSetupImgLoaderBvv imgLoader = new SourceToViewerSetupImgLoaderBvv( source );
		
		final FinalDimensions size = new FinalDimensions( source.getSource( 0, 0 ) );

		// configure time points
		int numTimepoints = 0;
		while( source.isPresent( numTimepoints ) )
			numTimepoints++;
		final ArrayList< TimePoint > timepoints = new ArrayList<>( numTimepoints );
		for ( int t = 0; t < numTimepoints; ++t )
			timepoints.add( new TimePoint( t ) );

		final HashMap< Integer, BasicViewSetup > setups = new HashMap<>( 1 );
		
		final BasicViewSetup setup = new BasicViewSetup( 0, source.getName(), size, source.getVoxelDimensions() );
		setups.put( 0, setup );
		final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal( new TimePoints( timepoints ), setups, imgLoader, null );
		final ArrayList< ViewRegistration > registrations = new ArrayList<>();
		for ( int t = 0; t < numTimepoints; ++t )
		{
			// The transforms are in the mipmap transforms
			// see SourceToViewerSetupImgLoaderBvv.mipmapTransforms
			// Thus, we currently don't add additional transforms here
			// TODO: probably better to split this up and only put the scaling transforms
			//       in the mipmaps and all the additional (rotation and translation) transformations here
			AffineTransform3D transform = new AffineTransform3D();
				registrations.add( new ViewRegistration( t, 0, transform ) );
		}

		return new AbstractSpimData( (File) null, seq, new ViewRegistrations( registrations) );
	}
}
