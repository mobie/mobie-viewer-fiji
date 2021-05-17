package de.embl.cba.mobie2.serialize;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import de.embl.cba.mobie2.view.additionalviews.AdditionalViews;
import de.embl.cba.tables.FileAndUrlUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;

import static de.embl.cba.mobie2.serialize.JsonHelper.buildGson;

public class AdditionalViewsJsonParser {
    public AdditionalViews getViews( String path ) throws IOException
    {
        final String s = FileAndUrlUtils.read( path );
        Gson gson = buildGson( false );
        Type type = new TypeToken< AdditionalViews >() {}.getType();
        return gson.fromJson( s, type );
    }

    public void saveViews( AdditionalViews additionalViews, String path ) throws IOException {
        Gson gson = buildGson( false );
        Type type = new TypeToken< AdditionalViews >() {}.getType();

        try ( OutputStream outputStream = new FileOutputStream( path );
             final JsonWriter writer = new JsonWriter( new OutputStreamWriter(outputStream, "UTF-8")) ) {
            writer.setIndent("	");
            gson.toJson(additionalViews, type, writer);
        }
    }

    public String viewsToJsonString( AdditionalViews additionalViews, boolean prettyPrinting ) {
        Gson gson = buildGson( true );
        Type type = new TypeToken< AdditionalViews >() {}.getType();
        return gson.toJson( additionalViews, type );
    }
}
