package de.embl.cba.mobie2.serialize;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.embl.cba.mobie2.view.AdditionalViews;
import de.embl.cba.tables.FileAndUrlUtils;

import java.io.IOException;
import java.lang.reflect.Type;

public class AdditionalViewsJsonParser {
    public AdditionalViews getViews( String path ) throws IOException
    {
        final String s = FileAndUrlUtils.read( path );
        Gson gson = new Gson();
        Type type = new TypeToken< AdditionalViews >() {}.getType();
        return gson.fromJson( s, type );
    }
}
