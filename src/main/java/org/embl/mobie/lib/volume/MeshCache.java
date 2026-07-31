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

import bdv.viewer.Source;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.lib.annotation.Segment;
import org.embl.mobie.lib.source.AnnotationType;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Disk-backed cache of pre-computed segment meshes.
 * <p>
 * All meshes for a given segmentation + resolution are stored in a single
 * {@code .mel} (mesh labels) binary file under the cache directory.
 * The in-memory map doubles as the dirty set — new entries are held in
 * memory until {@link #flush()} is called.
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
 * [for each segment:
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

	private final File cacheFile;
	private final ConcurrentHashMap< Long, float[] > meshes;

	/**
	 * Create (or open) a mesh cache for the given segmentation at the given
	 * resolution.  The cache file is loaded into memory immediately if it
	 * already exists on disk.
	 *
	 * @param cacheDir          root directory for all mesh caches
	 *                          (typically {@code ~/.mobie/mesh-cache/})
	 * @param segmentationName  human-readable name, e.g. {@code "nuclei"}
	 * @param smoothingIterations  smoothing level locked into the cached meshes
	 * @param voxelSpacing      rendering resolution in µm (first element used for filename)
	 */
	public MeshCache( File cacheDir, String segmentationName, int smoothingIterations, double[] voxelSpacing )
	{
		final String spacingStr = formatSpacing( voxelSpacing );
		final String fileName = segmentationName + "-sm" + smoothingIterations + "-" + spacingStr + ".mel";
		this.cacheFile = new File( cacheDir, fileName );
		this.meshes = new ConcurrentHashMap<>();
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

			final byte[] buf = new byte[ 12 ]; // label(8) + count(4) = 12 header bytes
			while ( true )
			{
				// read label ID + vertex count header
				readFully( in, buf, 0, 12 );
				final long label = bytesToLong( buf, 0 );
				final int count = bytesToInt( buf, 8 );
				if ( count < 0 )
					break; // sentinel: negative count marks end of stream

				// read vertex data
				final float[] vertices = new float[ count ];
				final byte[] vertBuf = new byte[ count * 4 ];
				readFully( in, vertBuf, 0, vertBuf.length );
				final ByteBuffer bb = ByteBuffer.wrap( vertBuf ).order( in instanceof java.io.DataInputStream ? ByteOrder.BIG_ENDIAN : ByteOrder.BIG_ENDIAN );
				bb.order( ByteOrder.BIG_ENDIAN );
				for ( int i = 0; i < vertices.length; i++ )
					vertices[ i ] = bb.getFloat();

				meshes.put( label, vertices );
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
	 * Write all cached meshes to disk.
	 */
	public synchronized void flush() throws IOException
	{
		cacheFile.getParentFile().mkdirs();

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final DataOutputStream out = new DataOutputStream( baos );

		out.writeInt( MAGIC );
		out.writeInt( VERSION );

		final byte[] header = new byte[ 12 ];
		for ( final java.util.Map.Entry< Long, float[] > entry : meshes.entrySet() )
		{
			final long label = entry.getKey();
			final float[] vertices = entry.getValue();

			longToBytes( label, header, 0 );
			intToBytes( vertices.length, header, 8 );
			out.write( header );

			final byte[] vertBuf = new byte[ vertices.length * 4 ];
			final ByteBuffer bb = ByteBuffer.wrap( vertBuf ).order( ByteOrder.BIG_ENDIAN );
			for ( float v : vertices )
				bb.putFloat( v );
			out.write( vertBuf );
		}

		out.flush();
		Files.write( cacheFile.toPath(), baos.toByteArray(),
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING );
	}

	// -- public API ----------------------------------------------------

	public boolean hasMesh( long labelId )
	{
		return meshes.containsKey( labelId );
	}

	@Nullable
	public float[] loadMesh( long labelId )
	{
		return meshes.get( labelId );
	}

	public void storeMesh( long labelId, float[] vertices )
	{
		meshes.put( labelId, vertices );
	}

	public int size()
	{
		return meshes.size();
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

	private static void longToBytes( long v, byte[] b, int off )
	{
		b[ off ] = ( byte ) ( v >>> 56 );
		b[ off + 1 ] = ( byte ) ( v >>> 48 );
		b[ off + 2 ] = ( byte ) ( v >>> 40 );
		b[ off + 3 ] = ( byte ) ( v >>> 32 );
		b[ off + 4 ] = ( byte ) ( v >>> 24 );
		b[ off + 5 ] = ( byte ) ( v >>> 16 );
		b[ off + 6 ] = ( byte ) ( v >>> 8 );
		b[ off + 7 ] = ( byte ) v;
	}

	private static void intToBytes( int v, byte[] b, int off )
	{
		b[ off ] = ( byte ) ( v >>> 24 );
		b[ off + 1 ] = ( byte ) ( v >>> 16 );
		b[ off + 2 ] = ( byte ) ( v >>> 8 );
		b[ off + 3 ] = ( byte ) v;
	}
}
