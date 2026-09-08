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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link MeshCache}: round-tripping through the .mel file,
 * appending across sessions, auto-persist when the in-memory map exceeds its
 * threshold, and overwriting a previously persisted label.
 */
class MeshCacheTest
{
	@TempDir
	File tempDir;

	private MeshCache newCache( int persistThreshold )
	{
		return new MeshCache( tempDir, "test", 5, new double[] { 0.5 }, persistThreshold );
	}

	private static float[] randomMesh( Random rng, int numVertices )
	{
		final float[] vertices = new float[ numVertices * 3 ];
		for ( int i = 0; i < vertices.length; i++ )
			vertices[ i ] = rng.nextFloat() * 100.0f;
		return vertices;
	}

	@Test
	void storesFlushesAndReloads() throws IOException
	{
		final Random rng = new Random( 1 );
		final float[] a = randomMesh( rng, 10 );
		final float[] b = randomMesh( rng, 25 );

		final MeshCache cache = newCache( 256 );
		assertFalse( cache.hasMesh( 1 ) );
		assertNull( cache.loadMesh( 1 ) );
		cache.storeMesh( 1, a );
		cache.storeMesh( 2, b );
		assertEquals( 2, cache.size() );
		assertArrayEquals( a, cache.loadMesh( 1 ) );
		cache.flush();

		final MeshCache reloaded = newCache( 256 );
		assertTrue( reloaded.hasMesh( 1 ) );
		assertTrue( reloaded.hasMesh( 2 ) );
		assertArrayEquals( a, reloaded.loadMesh( 1 ) );
		assertArrayEquals( b, reloaded.loadMesh( 2 ) );
		assertEquals( 2, reloaded.size() );
	}

	@Test
	void appendsAcrossInstances() throws IOException
	{
		final float[] a = randomMesh( new Random( 2 ), 7 );

		final MeshCache first = newCache( 256 );
		first.storeMesh( 1, a );
		first.flush();

		final MeshCache second = newCache( 256 );
		assertTrue( second.hasMesh( 1 ) );
		final float[] b = randomMesh( new Random( 3 ), 9 );
		second.storeMesh( 2, b );
		second.flush();

		final MeshCache third = newCache( 256 );
		assertArrayEquals( a, third.loadMesh( 1 ) );
		assertArrayEquals( b, third.loadMesh( 2 ) );
	}

	@Test
	void autoPersistsWhenThresholdExceeded()
	{
		final Random rng = new Random( 4 );
		final int threshold = 5;
		final MeshCache cache = newCache( threshold );

		// Store far more than the threshold without ever calling flush():
		// the cache must persist to disk on its own to stay memory-bounded,
		// while still serving all stored meshes.
		final java.util.Map< Long, float[] > expected = new java.util.HashMap<>();
		for ( long label = 1; label <= 50; label++ )
		{
			final float[] mesh = randomMesh( rng, 3 + ( int ) ( label % 10 ) );
			expected.put( label, mesh );
			cache.storeMesh( label, mesh );
		}
		assertEquals( 50, cache.size() );
		for ( long label = 1; label <= 50; label++ )
			assertArrayEquals( expected.get( label ), cache.loadMesh( label ) );

		// Everything must be readable back from disk alone.
		final MeshCache reloaded = newCache( threshold );
		assertEquals( 50, reloaded.size() );
		for ( long label = 1; label <= 50; label++ )
			assertArrayEquals( expected.get( label ), reloaded.loadMesh( label ) );
	}

	@Test
	void overwritesPreviouslyPersistedLabel() throws IOException
	{
		final float[] v1 = randomMesh( new Random( 5 ), 4 );
		final float[] v2 = randomMesh( new Random( 6 ), 12 );

		final MeshCache first = newCache( 256 );
		first.storeMesh( 1, v1 );
		first.flush();

		final MeshCache second = newCache( 256 );
		second.storeMesh( 1, v2 ); // same label, updated mesh
		assertArrayEquals( v2, second.loadMesh( 1 ) );
		second.flush();

		final MeshCache third = newCache( 256 );
		assertArrayEquals( v2, third.loadMesh( 1 ) ); // latest wins
	}
}
