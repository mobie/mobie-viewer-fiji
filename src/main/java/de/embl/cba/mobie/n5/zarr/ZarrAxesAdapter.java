package de.embl.cba.mobie.n5.zarr;

import com.google.gson.*;
import java.lang.reflect.Type;

// TODO - improve this business?
public class ZarrAxesAdapter implements JsonDeserializer< ZarrAxes >
    {

        @Override
        public ZarrAxes deserialize( JsonElement json, Type typeOfT, JsonDeserializationContext context ) throws JsonParseException
        {
            JsonArray array = json.getAsJsonArray();
            if ( array.size() > 0 ) {
                StringBuilder axisString = new StringBuilder("[");
                for ( int i=0; i<array.size(); i++ ) {
                    String element = array.get(i).getAsString();
                    if ( i != 0 ) {
                        axisString.append(",");
                    }
                    axisString.append("\"");
                    axisString.append(element);
                    axisString.append("\"");
                }
                axisString.append("]");
                return ZarrAxes.decode(axisString.toString());
            } else {
                return null;
            }
        }

        // @Override
        // public ZarrAxes serialize( List<SourceTransformer> sourceTransformers, Type type, JsonSerializationContext context ) {
        //     JsonArray ja = new JsonArray();
        //     for ( SourceTransformer sourceTransformer: sourceTransformers ) {
        //         Map< String, SourceTransformer > nameToTransformer = new HashMap<>();
        //         nameToTransformer.put( classToName.get( sourceTransformer.getClass().getName() ), sourceTransformer );
        //
        //         if ( sourceTransformer instanceof GridSourceTransformer ) {
        //             ja.add( context.serialize( nameToTransformer, new TypeToken< Map< String, GridSourceTransformer > >() {}.getType() ) );
        //         } else if ( sourceTransformer instanceof AffineSourceTransformer ) {
        //             ja.add( context.serialize( nameToTransformer , new TypeToken< Map< String, AffineSourceTransformer > >() {}.getType() ) );
        //         } else if ( sourceTransformer instanceof CropSourceTransformer ) {
        //             ja.add( context.serialize( nameToTransformer , new TypeToken< Map< String, CropSourceTransformer > >() {}.getType() ) );
        //         } else {
        //             throw new UnsupportedOperationException( "Could not serialise SourceTransformer of type: " + sourceTransformer.getClass().toString() );
        //         }
        //     }
        //
        //     return ja;
        // }
}
