package org.embl.mobie.lib.bvv;

import java.util.ArrayList;
import java.util.HashMap;

import net.imglib2.FinalDimensions;
import net.imglib2.realtransform.AffineTransform3D;

import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.viewer.Source;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;

public class BVVSourceToSpimDataWrapper
{
	/** wraps UnsignedByte or UnsignedShort source to a cached spimdata 
	 * (of UnsignedShort type) to display in BVV **/
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static AbstractSpimData< ? > spimDataSourceWrap(final Source<?> src_)
	{
		final BVVSourceToViewerSetupImgLoader imgLoader = new BVVSourceToViewerSetupImgLoader(src_);
		int numTimepoints = 0;
		
		final FinalDimensions size = new FinalDimensions( src_.getSource( 0, 0 ));
		
		while(src_.isPresent( numTimepoints ))
			numTimepoints++;

		final HashMap< Integer, BasicViewSetup > setups = new HashMap<>( 1 );
		
		final BasicViewSetup setup = new BasicViewSetup( 0, src_.getName(), size, src_.getVoxelDimensions() );
		setups.put( 0, setup );
		final ArrayList< TimePoint > timepoints = new ArrayList<>( numTimepoints );
		for ( int t = 0; t < numTimepoints; ++t )
			timepoints.add( new TimePoint( t ) );
		final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal( new TimePoints( timepoints ), setups, imgLoader, null );
		final ArrayList< ViewRegistration > registrations = new ArrayList<>();
		for ( int t = 0; t < numTimepoints; ++t )
		{
			AffineTransform3D transform = new AffineTransform3D();
			//scale transform already in the multires, no need
			//src_.getSourceTransform( t,0, transform );
			registrations.add( new ViewRegistration( t, 0, transform ) );
		}
		return new AbstractSpimData(null, seq, new ViewRegistrations( registrations));
	}
}
