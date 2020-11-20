/**
 * Copyright (c) 2019, Stephan Saalfeld
 * All rights reserved.
 *
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
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.embl.cba.mobie.n5.zarr;

import com.google.gson.*;
import org.janelia.saalfeldlab.n5.Bzip2Compression;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.blosc.BloscCompression;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 *
 */
public interface ZarrCompressor {

	/* idiotic stream based initialization because Java cannot have static initialization code in interfaces */
	public static Map< String, Class<? extends ZarrCompressor> > registry = Stream.of(
			new SimpleImmutableEntry<>("blosc", Blosc.class),
			new SimpleImmutableEntry<>("zlib", Zlib.class),
			new SimpleImmutableEntry<>("gzip", Gzip.class),
			new SimpleImmutableEntry<>("bz2", Bz2.class))
			.collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue));

	public static ZarrCompressor fromCompression( final Compression compression ) {

		try {
			if (compression instanceof BloscCompression) {
				return new Blosc((BloscCompression)compression);
			} else if (compression instanceof GzipCompression ) {
				final Class<? extends Compression > clazz = compression.getClass();
				final Field field = clazz.getDeclaredField("useZlib");
				field.setAccessible(true);
				final Boolean useZlib = ( Boolean )field.get(compression);
				field.setAccessible(false);
				return useZlib != null && useZlib ? new Zlib(( GzipCompression )compression) : new Gzip(( GzipCompression )compression);
			} else if (compression instanceof Bzip2Compression ) {
				return new Bz2(( Bzip2Compression )compression);
			} else {
				return null;
			}
		} catch ( NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			return null;
		}
	}

	public Compression getCompression();

	public static class Blosc implements ZarrCompressor {

		private final String id = "blosc";
		private final String cname;
		private final int clevel;
		private final int shuffle;
		private final int blocksize;
		private final transient int nthreads;

		public Blosc(
				final String cname,
				final int clevel,
				final int shuffle,
				final int blockSize,
				final int nthreads) {

			this.cname = cname;
			this.clevel = clevel;
			this.shuffle = shuffle;
			this.blocksize = blockSize;
			this.nthreads = nthreads;
		}

		public Blosc(final BloscCompression compression) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
		{

			final Class<? extends BloscCompression> clazz = compression.getClass();

			Field field = clazz.getDeclaredField("cname");
			field.setAccessible(true);
			cname = ( String )field.get(compression);
			field.setAccessible(false);

			field = clazz.getDeclaredField("clevel");
			field.setAccessible(true);
			clevel = field.getInt(compression);
			field.setAccessible(false);

			field = clazz.getDeclaredField("shuffle");
			field.setAccessible(true);
			shuffle = field.getInt(compression);
			field.setAccessible(false);

			field = clazz.getDeclaredField("blocksize");
			field.setAccessible(true);
			blocksize = field.getInt(compression);
			field.setAccessible(false);

			field = clazz.getDeclaredField("nthreads");
			field.setAccessible(true);
			nthreads = field.getInt(compression);
			field.setAccessible(false);
		}

		@Override
		public BloscCompression getCompression() {

			return new BloscCompression(cname, clevel, shuffle, blocksize, Math.max(1, nthreads));
		}
	}

	public static class Zlib implements ZarrCompressor {

		private final String id = "zlib";
		private final int level;

		public Zlib(final int level) {

			this.level = level;
		}

		public Zlib(final GzipCompression compression) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
		{

			final Class<? extends GzipCompression > clazz = compression.getClass();

			final Field field = clazz.getDeclaredField("level");
			field.setAccessible(true);
			level = field.getInt(compression);
			field.setAccessible(false);
		}

		@Override
		public GzipCompression getCompression() {

			return new GzipCompression(level, true);
		}
	}

	public static class Gzip implements ZarrCompressor {

		private final String id = "gzip";
		private final int level;

		public Gzip(final int level) {

			this.level = level;
		}

		public Gzip(final GzipCompression compression) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
		{

			final Class<? extends GzipCompression > clazz = compression.getClass();

			final Field field = clazz.getDeclaredField("level");
			field.setAccessible(true);
			level = field.getInt(compression);
			field.setAccessible(false);
		}

		@Override
		public GzipCompression getCompression() {

			return new GzipCompression(level);
		}
	}

	public static class Bz2 implements ZarrCompressor {

		private final String id = "bz2";
		private final int level;

		public Bz2(final int level) {

			this.level = level;
		}

		public Bz2(final Bzip2Compression compression) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
		{

			final Class<? extends Bzip2Compression > clazz = compression.getClass();

			final Field field = clazz.getDeclaredField("blockSize");
			field.setAccessible(true);
			level = field.getInt(compression);
			field.setAccessible(false);
		}

		@Override
		public Bzip2Compression getCompression() {

			return new Bzip2Compression(level);
		}
	}

	public static class Raw extends RawCompression implements ZarrCompressor {

		@Override
		public RawCompression getCompression() {

			return this;
		}
	}

	public static JsonAdapter jsonAdapter = new JsonAdapter();

	static public class JsonAdapter implements JsonDeserializer<ZarrCompressor> {

		@Override
		public ZarrCompressor deserialize( final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
				throws JsonParseException {

			final JsonObject jsonObject = json.getAsJsonObject();
			final JsonElement jsonId = jsonObject.get("id");
			if (jsonId == null)
				return null;
			final String id = jsonId.getAsString();
			final Class<? extends ZarrCompressor> compressorClass = registry.get(id);
			if (compressorClass == null)
				return null;

			return context.deserialize(json, compressorClass);
		}
	}
}
