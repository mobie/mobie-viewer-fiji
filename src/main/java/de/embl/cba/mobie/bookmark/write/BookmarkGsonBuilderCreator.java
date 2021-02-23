package de.embl.cba.mobie.bookmark.write;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class BookmarkGsonBuilderCreator
{
	public static Gson createGsonBuilder( boolean usePrettyPrinting) {
		// exclude the name field from json
		ExclusionStrategy strategy = new ExclusionStrategy() {
			@Override
			public boolean shouldSkipField( FieldAttributes f) {
				if (f.getName().equals("name") || f.getName().equals("type") ||
						f.getName().equals("storage") || f.getName().equals("tableFolder")) {
					return true;
				}
				return false;
			}

			@Override
			public boolean shouldSkipClass(Class<?> clazz) {
				return false;
			}
		};

		Gson gson;
		if (usePrettyPrinting) {
			gson = new GsonBuilder().setPrettyPrinting().addSerializationExclusionStrategy(strategy).create();
		} else {
			gson = new GsonBuilder().addSerializationExclusionStrategy(strategy).create();
		}
		return gson;
	}
}
