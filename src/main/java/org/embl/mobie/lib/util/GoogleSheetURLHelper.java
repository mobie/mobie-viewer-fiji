package org.embl.mobie.lib.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class GoogleSheetURLHelper
{
    // TODO: This should only be called once from TableOpener
    public static String generateExportUrl( String googleSheetUrl )
    {
        try
        {
            URL url = new URL( googleSheetUrl );
            String[] pathSegments = url.getPath().split( "/" );

            // Extract document ID from URL path
            String documentId = pathSegments[ 3 ];

            // Extract gid from the query string
            Map< String, String > queryParams = getQueryParams( url );
            String gid = queryParams.get( "gid" );

            // Construct the export URL
            if ( gid == null )
            {
                return String.format( "https://docs.google.com/spreadsheets/d/%s/export?format=%s", documentId, "tsv" );
            }
            else
            {
                return String.format( "https://docs.google.com/spreadsheets/d/%s/export?gid=%s&format=%s", documentId, gid, "tsv" );
            }
        }
        catch ( MalformedURLException e )
        {
            throw new RuntimeException( e );
        }
    }

    private static Map<String, String> getQueryParams( URL url) {
        Map<String, String> queryPairs = new HashMap<>();
        String[] pairs = url.getQuery().split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            queryPairs.put(pair.substring(0, idx), pair.substring(idx + 1));
        }
        return queryPairs;
    }

    public static void main( String[] args )
    {
        String googleSheetUrl = "https://docs.google.com/spreadsheets/d/1mSzFOAif2a7ki7-3yELdi68P7qdaWxBCCQdysh7R-rI/edit?gid=1627529093#gid=1627529093";
        String exportUrl = generateExportUrl( googleSheetUrl );
        System.out.println( "Export URL: " + exportUrl );

        googleSheetUrl = "https://docs.google.com/spreadsheets/d/1mSzFOAif2a7ki7-3yELdi68P7qdaWxBCCQdysh7R-rI/edit?usp=sharing";
        exportUrl = generateExportUrl( googleSheetUrl );
        System.out.println( "Export URL: " + exportUrl );
    }
}
