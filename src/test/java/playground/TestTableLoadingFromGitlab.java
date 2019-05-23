package playground;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class TestTableLoadingFromGitlab
{
	public static void main( String[] args ) throws IOException
	{
		URL oracle = new URL("https://git.embl.de/tischer/platy-browser-tables/raw/master/em-segmented-nuclei-labels.csv");
		BufferedReader in = new BufferedReader(
				new InputStreamReader(oracle.openStream()));

		String inputLine;

		while ((inputLine = in.readLine()) != null)
				System.out.println(inputLine);

		in.close();
	}
}
