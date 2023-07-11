package hcs;

import org.embl.mobie.lib.hcs.HCSPattern;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HCSPatternMatching
{
	public static void main( String[] args )
	{
		final Matcher matcher = Pattern.compile( HCSPattern.INCELL_WELL_SITE_CHANNEL ).matcher( HCSPattern.INCELL_EXAMPLE );
		System.out.println( "INCELL:" + matcher.matches() );
	}
}
