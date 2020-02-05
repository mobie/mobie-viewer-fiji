package explore;

import de.embl.cba.tables.Tables;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class ExploreTableLoadingFromGitlab
{
	public static void main( String[] args ) throws IOException
	{
		URL url = new URL("https://git.embl.de/tischer/platy-browser-tables/raw/dev/data/0.2.1/tables/sbem-6dpf-1-whole-segmented-nuclei-labels/default.csv");
		BufferedReader in = new BufferedReader( new InputStreamReader(url.openStream() )) ;

		String inputLine;
		while ((inputLine = in.readLine()) != null)
				System.out.println(inputLine);

		final BufferedReader reader = Tables.getReader( "https://git.embl.de/tischer/platy-browser-tables/raw/dev/data/0.2.1/tables/sbem-6dpf-1-whole-segmented-nuclei-labels/default.csv" );

		System.out.println( reader.readLine() );

		in.close();
	}
}
