/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package de.embl.cba.platynereis.labels.luts;

import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.AbstractIntegerType;
import net.imglib2.type.volatiles.VolatileARGBType;
import net.imglib2.type.volatiles.VolatileIntType;

import java.util.HashMap;
import java.util.Map;

/**
 * Conversion logic adapted from BigCat Viewer.
 */
public class VolatileIntTypeLabelsARGBConverter implements Converter< VolatileIntType, VolatileARGBType >
{
    private int alpha = 0x20000000;
    final static private double goldenRatio = 1.0 / ( 0.5 * Math.sqrt( 5 ) + 0.5 );
    private long seed = 50;
    final static private double[] rs = new double[]{ 1, 1, 0, 0, 0, 1, 1 };
    final static private double[] gs = new double[]{ 0, 1, 1, 1, 0, 0, 0 };
    final static private double[] bs = new double[]{ 0, 0, 0, 1, 1, 1, 0 };

    private Map< Long, Integer > lut = new HashMap<>();

    private int interpolate( final double[] xs, final int k, final int l, final double u, final double v ){
        return ( int )( ( v * xs[ k ] + u * xs[ l ] ) * 255.0 + 0.5 );
    }

    private int calculateARGB( final int r, final int g, final int b, final int alpha ) {
        return ( ( ( r << 8 ) | g ) << 8 ) | b | alpha;
    }

	@Override
	public void convert( final VolatileIntType input, final VolatileARGBType output )
	{

        double x = input.getRealDouble();
		long lx = (long) x;

        if(x != 0)
		{
			if ( lut.containsKey( lx ) )
			{
				output.setValid( true );
				output.set( lut.get( lx ) );
			}
			else
			{
				x = ( x * seed ) * goldenRatio;
				x = x - ( long ) Math.floor( x );
				x *= 6.0;
				final int k = ( int ) x;
				final int l = k + 1;
				final double u = x - k;
				final double v = 1.0 - u;
				final int red = interpolate( rs, k, l, u, v );
				final int green = interpolate( gs, k, l, u, v );
				final int blue = interpolate( bs, k, l, u, v );
				int argb = calculateARGB( red, green, blue, alpha );
				final double alpha = ARGBType.alpha( argb );
				final int aInt = Math.min( 255, ( int ) ( alpha ) );
				final int rInt = Math.min( 255, ARGBType.red( argb ) );
				final int gInt = Math.min( 255, ARGBType.green( argb ) );
				final int bInt = Math.min( 255, ARGBType.blue( argb ) );
				output.setValid( true );
				final int color = ( ( ( ( ( aInt << 8 ) | rInt ) << 8 ) | gInt ) << 8 ) | bInt;
				output.set( color );
				lut.put( lx, color );
			}
        }
        else
		{
            output.set(0);
        }
	}
}
