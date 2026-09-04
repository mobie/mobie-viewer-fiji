/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
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
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
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
package org.embl.mobie.lib.volume;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Disk-backed cache of pre-computed segment meshes.
 * <p>
 * All meshes for a given segmentation + resolution are stored in a single
 * {@code .mel} (mesh labels) binary file under the cache directory.
 * <p>
 * <h3>Memory-bounded design</h3>
 * The cache never holds all meshes in RAM. When opened, only a small in-memory
 * index (label -&gt; file offset) is built; mesh data is read from disk on
 * demand. Newly stored meshes are buffered in memory and appended to the file
 * whenever the buffer exceeds {@link #DEFAULT_PERSIST_THRESHOLD} entries (or on
 * {@link #flush()}), keeping peak memory low even for very large runs.
 *
 * <h3>Cache directory layout</h3>
 * <pre>
 * ~/.mobie/mesh-cache/
 *     nuclei-sm5-1_000um.mel
 *     cells-sm5-0_500um.mel
 * </pre>
 *
 * <h3>File format</h3>
 * <pre>
 * [4 bytes magic:  0x4D 0x45 0x4C 0x48  ("MELH")]
 * [4 bytes version: 1]
 * [for each segment (records are appended, never rewritten):
 *     [8 bytes int64: label ID]
 *     [4 bytes uint32: vertex count N]
 *     [N * 12 bytes: float x0, y0, z0, x1, y1, z1, ...]
 * ]
 * </pre>
 */
public class MeshCache
{
	private static final int MAGIC = 0x4D454C48;  // "MELH"
	private static final int VERSION = 1;

	/** Number of buffered, not-yet-appended meshes before an automatic persist. */
	private static final int DEFAULT_PERSIST_THRESHOLD = 256;

	private final File cacheFile;

	/** Meshes stored but not yet appended to the file; bounded by the persist threshold. */
	private final ConcurrentHashMap< Long, float[] > dirtyMeshes;

	/** label -&gt; { byte offset of the record in the file, vertex count }. */
	private final ConcurrentHashMap< Long, long[] > persistedIndex;

	private final int persistThreshold;

	/**
	 * Create (or open) a mesh cache for the given segmentation at the given
	 * resolution.
	 *
	 * @param cacheDir          root directory for all mesh caches
	 *                          (typically {@code ~/.mobie/mesh-cache/})
	 * @param segmentationName  human-readable name, e.g. {@code "nuclei"}
	 * @param smoothingIterations  smoothing level locked into the cached meshes
	 * @param voxelSpacing      rendering resolution in µm (first element used for filename)
	 */
	public MeshCache( File cacheDir, String segmentationName, int smoothingIterations, double[] voxelSpacing )
	{
		this( cacheDir, segmentationName, smoothingIterations, voxelSpacing, DEFAULT_PERSIST_THRESHOLD );
	}

	/**
	 * Package-private constructor with an explicit persist threshold (used by tests).
	 */
	MeshCache( File cacheDir, String segmentationName, int smoothingIterations, double[] voxelSpacing, int persistThreshold )
	{
		final String spacingStr = formatSpacing( voxelSpacing );
		final String fileName = segmentationName + "-sm" + smoothingIterations + "-" + spacingStr + ".mel";
		this.cacheFile = new File( cacheDir, fileName );
		this.persistThreshold = Math.max( 1, persistThreshold );
		this.dirtyMeshes = new ConcurrentHashMap<>();
		this.persistedIndex = new ConcurrentHashMap<>();
		load();
	}

	private static String formatSpacing( double[] voxelSpacing )
	{
		if ( voxelSpacing == null || voxelSpacing.length == 0 )
			return "auto";

		final String raw = String.format( "%.3f", voxelSpacing[ 0 ] );
		// strip trailing zeros, then replace the dot with underscore
		String trimmed = raw.replaceAll( "0+$", "" );
		if ( trimmed.endsWith( "." ) )
			trimmed = trimmed.substring( 0, trimmed.length() - 1 );
		return trimmed.replace( '.', '_' ) + "um";
	}

	// -- read/write ----------------------------------------------------

	/**
	 * Scan the cache file and build the in-memory label index without loading
	 * any vertex data.
	 */
	private void load()
	{
		if ( ! cacheFile.exists() )
			return;

		try ( final DataInputStream in = new DataInputStream(
				new BufferedInputStream( new FileInputStream( cacheFile ) ) ) )
		{
			final int magic = in.readInt();
			if ( magic != MAGIC )
				throw new IOException( "Bad magic: " + Integer.toHexString( magic ) );

			final int version = in.readInt();
			if ( version != VERSION )
				throw new IOException( "Unsupported version: " + version );

			final byte[] header = new byte[ 12 ];
			long offset = 8; // magic + version
			while ( true )
			{
				readFully( in, header, 0, 12 );
				final long label = bytesToLong( header, 0 );
				final int count = bytesToInt( header, 8 );
				if ( count < 0 )
					break; // sentinel: negative count marks end of stream

				// Only index records whose payload could be read completely.
				skipFully( in, ( long ) count * 4 );
				persistedIndex.put( label, new long[] { offset, count } );
				offset += 12 + ( long ) count * 4;
			}
		}
		catch ( EOFException e )
		{
			// normal end of file
		}
		catch ( IOException e )
		{
			System.err.println( "[MeshCache] Failed to load " + cacheFile.getAbsolutePath() + ": " + e.getMessage() );
		}
	}

	/**
	 * Append all currently buffered meshes to the cache file.
	 */
	private synchronized void persistDirty() throws IOException
	{
		if ( dirtyMeshes.isEmpty() )
			return;

		cacheFile.getParentFile().mkdirs();

		try ( final FileChannel channel = FileChannel.open(
				cacheFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE ) )
		{
			if ( channel.size() == 0 )
			{
				channel.write( ByteBuffer.wrap( new byte[] {
						( byte ) ( MAGIC >>> 24 ), ( byte ) ( MAGIC >>> 16 ),
						( byte ) ( MAGIC >>> 8 ), ( byte ) MAGIC,
						( byte ) ( VERSION >>> 24 ), ( byte ) ( VERSION >>> 16 ),
						( byte ) ( VERSION >>> 8 ), ( byte ) VERSION } ) );
			}

			final ByteBuffer record = ByteBuffer.allocate( 12 ).order( ByteOrder.BIG_ENDIAN );
			for ( final Map.Entry< Long, float[] > entry : dirtyMeshes.entrySet() )
			{
				final long recordStart = channel.size();
				channel.position( recordStart );

				final float[] vertices = entry.getValue();
				record.clear();
				record.putLong( entry.getKey() );
				record.putInt( vertices.length );
				record.flip();
				writeFully( channel, record );

				final ByteBuffer verticesBuffer = ByteBuffer.allocate( vertices.length * 4 ).order( ByteOrder.BIG_ENDIAN );
				verticesBuffer.asFloatBuffer().put( vertices );
				writeFully( channel, verticesBuffer );

				persistedIndex.put( entry.getKey(), new long[] { recordStart, vertices.length } );
			}
			channel.force( false );
		}

		dirtyMeshes.clear();
	}

	private static void writeFully( final FileChannel channel, final ByteBuffer buffer ) throws IOException
	{
		while ( buffer.hasRemaining() )
			channel.write( buffer );
	}

	private float[] readPersisted( long label, long recordOffset, int count )
	{
		try ( final FileChannel channel = FileChannel.open( cacheFile.toPath(), StandardOpenOption.READ ) )
		{
			final ByteBuffer verticesBuffer = ByteBuffer.allocate( count * 4 ).order( ByteOrder.BIG_ENDIAN );
			long position = recordOffset + 12;
			while ( verticesBuffer.hasRemaining() )
			{
				final int read = channel.read( verticesBuffer, position );
				if ( read < 0 )
					throw new EOFException( "Unexpected end of file while reading label " + label );
				position += read;
			}
			verticesBuffer.flip();
			final float[] vertices = new float[ count ];
			verticesBuffer.asFloatBuffer().get( vertices );
			return vertices;
		}
		catch ( IOException e )
		{
			System.err.println( "[MeshCache] Failed to read label " + label + " from " + cacheFile.getAbsolutePath() + ": " + e.getMessage() );
			return null;
		}
	}

	/**
	 * Write all buffered meshes to disk.
	 */
	public synchronized void flush() throws IOException
	{
		persistDirty();
	}

	// -- public API ----------------------------------------------------

	public boolean hasMesh( long labelId )
	{
		return dirtyMeshes.containsKey( labelId ) || persistedIndex.containsKey( labelId );
	}

	@javax.annotation.Nullable
	public float[] loadMesh( long labelId )
	{
		final float[] mesh = dirtyMeshes.get( labelId );
		if ( mesh != null )
			return mesh;

		final long[] record = persistedIndex.get( labelId );
		if ( record == null )
			return null;

		return readPersisted( labelId, record[ 0 ], ( int ) record[ 1 ] );
	}

	public synchronized void storeMesh( long labelId, float[] vertices )
	{
		dirtyMeshes.put( labelId, vertices );
		if ( dirtyMeshes.size() >= persistThreshold )
		{
			try
			{
				persistDirty();
			}
			catch ( IOException e )
			{
				System.err.println( "[MeshCache] Failed to persist " + cacheFile.getAbsolutePath() + ": " + e.getMessage() );
			}
		}
	}

	public int size()
	{
		return dirtyMeshes.size() + persistedIndex.size();
	}

	/**
	 * Scan {@code cacheRoot} for mesh-cache files of the given segmentation
	 * (any smoothing level) and return the smallest (finest) spacing found, or
	 * {@code null} if there is no cache file for this segmentation.
	 *
	 * @param cacheRoot        root cache directory
	 * @param segmentationName name used in the cache file name
	 */
	public static Double findFinestAvailableSpacing( File cacheRoot, String segmentationName )
	{
		if ( cacheRoot == null || ! cacheRoot.isDirectory() )
			return null;

		final Pattern pattern = Pattern.compile(
				Pattern.quote( segmentationName ) + "-sm\\d+-([0-9_]+)um\\.mel" );

		final File[] files = cacheRoot.listFiles();
		if ( files == null )
			return null;

		Double best = null;
		for ( final File file : files )
		{
			final Matcher matcher = pattern.matcher( file.getName() );
			if ( ! matcher.matches() )
				continue;
			final double spacing = Double.parseDouble( matcher.group( 1 ).replace( '_', '.' ) );
			if ( best == null || spacing < best )
				best = spacing;
		}
		return best;
	}

	/**
	 * Like {@link #findFinestAvailableSpacing(File, String)} but restricted to
	 * cache files created with the given smoothing iteration count.
	 */
	public static Double findFinestAvailableSpacing( File cacheRoot, String segmentationName, int smoothingIterations )
	{
		if ( cacheRoot == null || ! cacheRoot.isDirectory() )
			return null;

		final Pattern pattern = Pattern.compile(
				Pattern.quote( segmentationName ) + "-sm" + smoothingIterations + "-([0-9_]+)um\\.mel" );

		final File[] files = cacheRoot.listFiles();
		if ( files == null )
			return null;

		Double best = null;
		for ( final File file : files )
		{
			final Matcher matcher = pattern.matcher( file.getName() );
			if ( ! matcher.matches() )
				continue;
			final double spacing = Double.parseDouble( matcher.group( 1 ).replace( '_', '.' ) );
			if ( best == null || spacing < best )
				best = spacing;
		}
		return best;
	}

	// -- helpers -------------------------------------------------------

	private static void readFully( final InputStream in, final byte[] buf, final int off, final int len ) throws IOException
	{
		int total = 0;
		while ( total < len )
		{
			final int r = in.read( buf, off + total, len - total );
			if ( r < 0 )
				throw new EOFException();
			total += r;
		}
	}

	private static void skipFully( final InputStream in, final long len ) throws IOException
	{
		long remaining = len;
		while ( remaining > 0 )
		{
			final long skipped = in.skip( remaining );
			if ( skipped <= 0 )
			{
				if ( in.read() < 0 )
					throw new EOFException();
				remaining--;
			}
			else
			{
				remaining -= skipped;
			}
		}
	}

	private static long bytesToLong( byte[] b, int off )
	{
		return ( ( long ) ( b[ off ] & 0xFF ) << 56 )
				| ( ( long ) ( b[ off + 1 ] & 0xFF ) << 48 )
				| ( ( long ) ( b[ off + 2 ] & 0xFF ) << 40 )
				| ( ( long ) ( b[ off + 3 ] & 0xFF ) << 32 )
				| ( ( long ) ( b[ off + 4 ] & 0xFF ) << 24 )
				| ( ( long ) ( b[ off + 5 ] & 0xFF ) << 16 )
				| ( ( long ) ( b[ off + 6 ] & 0xFF ) << 8 )
				| ( b[ off + 7 ] & 0xFF );
	}

	private static int bytesToInt( byte[] b, int off )
	{
		return ( ( b[ off ] & 0xFF ) << 24 )
				| ( ( b[ off + 1 ] & 0xFF ) << 16 )
				| ( ( b[ off + 2 ] & 0xFF ) << 8 )
				| ( b[ off + 3 ] & 0xFF );
	}
}
