/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
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
package users.sultan;

//import bvv.util.BvvFunctions;
//import bvv.util.BvvOptions;
//import bvv.util.BvvStackSource;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imglib2.type.numeric.ARGBType;

import java.util.List;

public class ViewNeuropils3D
{

	public static void main( String[] args ) throws SpimDataException
	{
		SpimData platyBrowserNeuropil = new XmlIoSpimData().load( "/Volumes/cba/exchange/Sultan/prospr_neuropile_0.4um.xml" );
		SpimData xRayNeuropil = new XmlIoSpimData().load( "/Volumes/cba/exchange/Sultan/platy_90_02_neuropile_1um.xml" );
		SpimData xRayNeuropilAligned = new XmlIoSpimData().load( "/Volumes/cba/exchange/Sultan/platy_90_02_neuropile_1um-transform.xml-aligned.xml" );


//		final List< BvvStackSource< ? > > show = BvvFunctions.show( xRayNeuropil );
//		show.get( 0 ).setDisplayRange( 0, 65535 );
//
//		final List< BvvStackSource< ? > > show2 = BvvFunctions.show( platyBrowserNeuropil, BvvOptions.options().addTo( show.get( 0 ).getBvvHandle() ) );
//		show2.get( 0 ).setDisplayRange( 0, 255 );
//		show2.get( 0 ).setColor( new ARGBType( 0xff00ff00 ) );
//
//		final List< BvvStackSource< ? > > show3 = BvvFunctions.show( xRayNeuropilAligned, BvvOptions.options().addTo( show.get( 0 ).getBvvHandle() ) );
//		show3.get( 0 ).setDisplayRange( 0, 255 );
//		show3.get( 0 ).setColor( new ARGBType( 0xff00ffff ) );

	}
}
