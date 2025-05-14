package org.embl.mobie.lib.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class GoogleSheetURLHelper
{

    public static String generateExportUrl( String googleSheetUrl )
    {
        try
        {
            URL url = new URL( googleSheetUrl );
            String path = url.getPath();

            // Match and keep the "/d/{id}" or "/d/e/{id}" part for export URL construction
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(/d/(?:e/)?[\\w-]+)");
            java.util.regex.Matcher matcher = pattern.matcher(path);

            if (!matcher.find()) {
                throw new RuntimeException("Could not extract document path from URL: " + googleSheetUrl);
            }

            String documentPath = matcher.group(1);

            // Extract gid from the query string if query exists
            String gid = null;
            if (url.getQuery() != null) {
                Map<String, String> queryParams = getQueryParams(url);
                gid = queryParams.get("gid");
            }

            // Construct the export URL
            if (gid == null)
            {
                return String.format("https://docs.google.com/spreadsheets%s/export?format=%s", documentPath, "tsv");
            }
            else
            {
                return String.format("https://docs.google.com/spreadsheets%s/export?gid=%s&format=%s", documentPath, gid, "tsv");
            }
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
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

        // FIXME: opening the below URL within MoBIE does not work
        googleSheetUrl = "https://docs.google.com/spreadsheets/d/e/2PACX-1vThEdzde-UEIeXz8paAycUrPjzeU3eqfIGEfVl_usONhplBp6CczjCK99GFCKpA6l2vjJ-r_EHfUIlK/pub?gid=499533529";
        exportUrl = generateExportUrl( googleSheetUrl );
        System.out.println( "Export URL: " + exportUrl );

        googleSheetUrl = "https://docs.google.com/spreadsheets/d/1Yphl5FHoU2DcP7CeeTRUTyi3eXwr0BKuARKQsK4sFw0/export?format=tsv";
        exportUrl = generateExportUrl( googleSheetUrl );
        System.out.println( "Export URL: " + exportUrl );

        googleSheetUrl = "https://docs.google.com/spreadsheets/d/1Yphl5FHoU2DcP7CeeTRUTyi3eXwr0BKuARKQsK4sFw0";
        exportUrl = generateExportUrl( googleSheetUrl );
        System.out.println( "Export URL: " + exportUrl );
    }
}
