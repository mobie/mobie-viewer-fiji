package org.embl.mobie.lib.util;

import java.util.Arrays;

public class Corners
{
    public double[] ul = new double[ 3 ];
    public double[] ur = new double[ 3 ];
    public double[] ll = new double[ 3 ];
    public double[] lr = new double[ 3 ];

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append( "Upper left: " + Arrays.toString( ul ) + "\n" );
        builder.append( "Upper right: " + Arrays.toString( ur ) + "\n" );
        builder.append( "Lower left: " + Arrays.toString( ll ) + "\n" );
        builder.append( "Lower right: " + Arrays.toString( lr ) + "\n" );
        return builder.toString();
    }
}
