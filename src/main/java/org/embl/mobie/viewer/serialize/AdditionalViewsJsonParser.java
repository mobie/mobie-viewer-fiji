package org.embl.mobie.viewer.serialize;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import org.embl.mobie.viewer.view.additionalviews.AdditionalViews;
import de.embl.cba.tables.FileAndUrlUtils;

import java.io.*;
import java.lang.reflect.Type;

public class AdditionalViewsJsonParser {
    public AdditionalViews getViews(String path ) throws IOException
    {
        final String s = FileAndUrlUtils.read( path );
        Gson gson = JsonHelper.buildGson( false );
        Type type = new TypeToken< AdditionalViews >() {}.getType();
        return gson.fromJson( s, type );
    }

    public void saveViews( AdditionalViews additionalViews, String path ) throws IOException {
        Gson gson = JsonHelper.buildGson( false );
        Type type = new TypeToken< AdditionalViews >() {}.getType();

        File parentDir = new File( path ).getParentFile();
        if ( !parentDir.exists() ) {
            parentDir.mkdirs();
        }

        try ( OutputStream outputStream = new FileOutputStream( path );
             final JsonWriter writer = new JsonWriter( new OutputStreamWriter(outputStream, "UTF-8")) ) {
            writer.setIndent("  ");
            gson.toJson(additionalViews, type, writer);
        }
    }

    public String viewsToJsonString( AdditionalViews additionalViews, boolean prettyPrinting ) {
        Gson gson = JsonHelper.buildGson( true );
        Type type = new TypeToken< AdditionalViews >() {}.getType();
        return gson.toJson( additionalViews, type );
    }
}
