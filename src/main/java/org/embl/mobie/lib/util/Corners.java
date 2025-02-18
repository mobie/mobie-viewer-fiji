package org.embl.mobie.lib.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Arrays;

public class Corners
{
    public double[] upperLeft = new double[ 3 ];
    public double[] upperRight = new double[ 3 ];
    public double[] lowerLeft = new double[ 3 ];
    public double[] lowerRight = new double[ 3 ];

    @Override
    public String toString()
    {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(this);
//        StringBuilder builder = new StringBuilder();
//        builder.append( "Upper left: " + Arrays.toString( upperLeft ) + "\n" );
//        builder.append( "Upper right: " + Arrays.toString( upperRight ) + "\n" );
//        builder.append( "Lower left: " + Arrays.toString( lowerLeft ) + "\n" );
//        builder.append( "Lower right: " + Arrays.toString( lowerRight ) + "\n" );
//        return builder.toString();
    }
}
