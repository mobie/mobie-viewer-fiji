/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
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
package projects.sultan;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.Source;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import java.util.List;

public class ViewNeuropils
{
	public static void main( String[] args ) throws SpimDataException
	{
		SpimData platyBrowserNeuropil = new XmlIoSpimData().load( "/Volumes/cba/exchange/Sultan/prospr_neuropile_0.4um.xml" );
		SpimData xRayNeuropil = new XmlIoSpimData().load( "/Volumes/cba/exchange/Sultan/platy_90_02_neuropile_1um.xml" );


		final List< BdvStackSource< ? > > show = BdvFunctions.show( platyBrowserNeuropil );
		final Source< ? > spimSource = show.get( 0 ).getSources().get( 0 ).getSpimSource();
		BdvFunctions.show( xRayNeuropil, BdvOptions.options().addTo( show.get( 0 ) ) );

		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( show.get( 0 ).getBdvHandle().getTriggerbindings(), "behaviours" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {

			(new Thread( () -> {
				final TransformedSource transformedSource = ( TransformedSource ) spimSource;
				final AffineTransform3D fixed = new AffineTransform3D();
				transformedSource.getFixedTransform( fixed );
				System.out.println( "Fixed: " + fixed.toString() );

				final AffineTransform3D incr = new AffineTransform3D();
				transformedSource.getIncrementalTransform( incr );
				System.out.println( "Incr: " + incr.toString() );

				final AffineTransform3D whole = new AffineTransform3D();
				transformedSource.getSourceTransform( 0, 0, whole );
				System.out.println( "Whole: " + whole.toString() );

			} )).start();

		}, "Print position and view", "P"  ) ;

	}
}
