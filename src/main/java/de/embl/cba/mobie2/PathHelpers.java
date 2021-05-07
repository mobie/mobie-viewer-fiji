package de.embl.cba.mobie2;

import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.github.GitHubUtils;

public class PathHelpers {
    public static String getPath( String rootLocation, String githubBranch, String... files )
    {
        if ( rootLocation.contains( "github.com" ) )
        {
            rootLocation = GitHubUtils.createRawUrl( rootLocation, githubBranch );
        }

        final String[] strings = new String[ files.length + 2 ];
        strings[ 0 ] = rootLocation;
        strings[ 1 ] = "data";
        for ( int i = 0; i < files.length; i++ )
        {
            strings[ i + 2] = files[ i ];
        }

        String path = FileAndUrlUtils.combinePath( strings );

        return path;
    }
}
