package org.embl.mobie.lib.transform.elastix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Temporary local copy of ITC elastix parsing support.
 * Remove once MoBIE can depend on a released ITC version with inverse BSpline support.
 */
public class ElastixTransform
{
	public static final String BSPLINE_TRANSFORM = "BSplineTransform";

	public String Transform;
	public Integer NumberOfParameters;
	public Double[] TransformParameters;
	public Integer FixedImageDimension;
	public Integer MovingImageDimension;
	public Integer[] Size;
	public Double[] Spacing;
	public Double[] Origin;

	public static ElastixTransform load( final File file ) throws IOException
	{
		final Pattern pattern = Pattern.compile( "\\((\\S+)\\s(.+)(\\))" );
		ElastixBSplineTransform transform = null;

		try ( BufferedReader reader = new BufferedReader( new FileReader( file ) ) )
		{
			String line;
			while ( ( line = reader.readLine() ) != null )
			{
				final Matcher matcher = pattern.matcher( line );
				if ( !matcher.matches() )
					continue;
				if ( "Transform".equals( matcher.group( 1 ) ) )
				{
					final String transformType = stripQuotes( matcher.group( 2 ) );
					if ( !BSPLINE_TRANSFORM.equals( transformType ) )
						throw new UnsupportedOperationException( "Unsupported transform type: " + transformType );
					transform = new ElastixBSplineTransform();
					transform.Transform = transformType;
					break;
				}
			}
		}

		if ( transform == null )
			throw new UnsupportedOperationException( "Could not find transform type in file: " + file.getAbsolutePath() );

		try ( BufferedReader reader = new BufferedReader( new FileReader( file ) ) )
		{
			String line;
			while ( ( line = reader.readLine() ) != null )
			{
				final Matcher matcher = pattern.matcher( line );
				if ( !matcher.matches() )
					continue;

				final String key = matcher.group( 1 );
				if ( "Transform".equals( key ) )
					continue;

				assign( transform, key, matcher.group( 2 ) );
			}
		}

		if ( transform.FixedImageDimension == null || transform.MovingImageDimension == null )
			throw new UnsupportedOperationException( "Missing dimension fields in file: " + file.getAbsolutePath() );
		if ( !transform.FixedImageDimension.equals( transform.MovingImageDimension ) )
			throw new UnsupportedOperationException( "Fixed and moving dimensions differ" );

		return transform;
	}

	private static void assign( final ElastixBSplineTransform transform, final String key, final String value )
	{
		switch ( key )
		{
			case "NumberOfParameters":
				transform.NumberOfParameters = parseInteger( value );
				break;
			case "TransformParameters":
				transform.TransformParameters = parseDoubleArray( value );
				break;
			case "FixedImageDimension":
				transform.FixedImageDimension = parseInteger( value );
				break;
			case "MovingImageDimension":
				transform.MovingImageDimension = parseInteger( value );
				break;
			case "Size":
				transform.Size = parseIntegerArray( value );
				break;
			case "Spacing":
				transform.Spacing = parseDoubleArray( value );
				break;
			case "Origin":
				transform.Origin = parseDoubleArray( value );
				break;
			case "GridSize":
				transform.GridSize = parseIntegerArray( value );
				break;
			case "GridSpacing":
				transform.GridSpacing = parseDoubleArray( value );
				break;
			case "GridOrigin":
				transform.GridOrigin = parseDoubleArray( value );
				break;
			case "BSplineTransformSplineOrder":
				transform.BSplineTransformSplineOrder = parseInteger( value );
				break;
			default:
				// Ignore unsupported elastix keys that MoBIE does not currently consume.
		}
	}

	private static Integer parseInteger( final String value )
	{
		return Integer.valueOf( value.trim() );
	}

	private static Integer[] parseIntegerArray( final String value )
	{
		final String[] tokens = splitTokens( value );
		final Integer[] parsed = new Integer[ tokens.length ];
		for ( int i = 0; i < tokens.length; i++ )
			parsed[ i ] = Integer.valueOf( tokens[ i ] );
		return parsed;
	}

	private static Double[] parseDoubleArray( final String value )
	{
		final String[] tokens = splitTokens( value );
		final Double[] parsed = new Double[ tokens.length ];
		for ( int i = 0; i < tokens.length; i++ )
			parsed[ i ] = Double.valueOf( tokens[ i ] );
		return parsed;
	}

	private static String[] splitTokens( final String value )
	{
		return value.trim().split( "\\s+" );
	}

	private static String stripQuotes( final String value )
	{
		final String trimmed = value.trim();
		if ( trimmed.length() >= 2 && trimmed.startsWith( "\"" ) && trimmed.endsWith( "\"" ) )
			return trimmed.substring( 1, trimmed.length() - 1 );
		return trimmed;
	}
}

