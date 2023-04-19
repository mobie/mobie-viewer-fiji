package develop;

import org.embl.mobie.lib.hcs.HCSScheme;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HCSOMEZarrParsing
{
	public static void main( String[] args )
	{
		final HCSScheme omeZarr = HCSScheme.OMEZarr;
//		final String regex = ".*.zarr/(?<well>[A-Z]/[0-9]+)/(?<site>[0-9]+)/.*";
		//final String regex = ".*.zarr/(?<well>[A-Z]/[0-9]+)/(?<site>[0-9]+)/.*";
		final Matcher matcher = Pattern.compile( ".*.zarr/(?<well>[A-Z]/[0-9]+)/(?<site>[0-9]+)$" ).matcher( "/g/cba/exchange/hcs-test/hcs-ome.zarr/A/1/0" );
		final boolean matches = matcher.matches();
		final boolean b = omeZarr.setPath( "/g/cba/exchange/hcs-test/hcs-ome.zarr/A/1/0" );
		int a = 1;
	}
}
