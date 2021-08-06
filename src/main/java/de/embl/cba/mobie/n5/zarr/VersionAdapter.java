package de.embl.cba.mobie.n5.zarr;

import com.google.api.client.json.JsonString;
import com.google.gson.*;
import org.janelia.saalfeldlab.n5.N5Reader;

import java.lang.reflect.Type;

public class VersionAdapter implements JsonDeserializer< N5Reader.Version >, JsonSerializer< N5Reader.Version >
{

    @Override
    public N5Reader.Version deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context ) throws JsonParseException
    {
        String[] versionElements = json.getAsString().split("\\.");
        int[] versionNumbers = new int[versionElements.length];

        for ( int i = 0; i<versionNumbers.length; i++) {
            versionNumbers[i] = Integer.parseInt(versionElements[i]);
        }

        if ( versionNumbers.length > 2 ) {
            return new N5Reader.Version(versionNumbers[0], versionNumbers[1], versionNumbers[2]);
        } else {
            return new N5Reader.Version(versionNumbers[0], versionNumbers[1], 0);
        }
    }

    @Override
    public JsonElement serialize(N5Reader.Version src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive( src.getMajor() + "." + src.getMinor() + "." + src.getPatch() );
    }
}
