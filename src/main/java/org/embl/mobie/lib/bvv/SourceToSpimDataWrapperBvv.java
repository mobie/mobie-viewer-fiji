package org.embl.mobie.lib.bvv;

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

public class SourceToSpimDataWrapperBvv
{
	/** wraps UnsignedByte, UnsignedShort, UnsignedLong or Float type source to a cached spimdata 
	 * (of UnsignedShort type) to display in BVV, otherwise returns null **/
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static AbstractSpimData< ? > spimDataSourceWrap(final Source<?> src_)
	{		
		Object type = Util.getTypeFromInterval( src_.getSource( 0, 0 ) );
		
		if(!(type instanceof RealType  && type instanceof NativeType))
		{
			//System.err.println( "Volume view of image of type " + type + " is currently not supported.");
			return null;
		}
		final SourceToViewerSetupImgLoaderBvv imgLoader = new SourceToViewerSetupImgLoaderBvv(src_);
		
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

		return new AbstractSpimData( null, seq, new ViewRegistrations( registrations) );
	}
}
