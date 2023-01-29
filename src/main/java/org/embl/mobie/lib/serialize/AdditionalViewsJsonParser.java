/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.lib.serialize;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import org.embl.mobie.lib.view.AdditionalViews;
import org.embl.mobie.io.util.IOHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;

public class AdditionalViewsJsonParser {
    public AdditionalViews getViews( String path ) throws IOException
    {
        final String s = IOHelper.read( path );
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
