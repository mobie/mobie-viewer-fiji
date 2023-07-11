/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
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
package develop;

import org.embl.mobie.lib.ThreadHelper;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BenchmarkMultithreadedTableSawTableLoading
{
	public static void main( String[] args ) throws IOException
	{
		long start;

		Table.read().usingOptions( CsvReadOptions.builderFromString( "aaa\tbbb" ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" ) );

		final ArrayList< Future< ? > > futures = ThreadHelper.getFutures();
		final ExecutorService ioExecutorService = Executors.newFixedThreadPool( 2 );
		start = System.currentTimeMillis();
		int n = 2;
		for ( int i = 0; i < n; i++ )
		{
			futures.add(
				ioExecutorService.submit( () ->
					{
						CsvReadOptions.Builder builder = CsvReadOptions.builder( new File( "/Users/tischer/Desktop/default_regions.tsv" ) ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" );
						Table.read().usingOptions( builder );
					}
			) );
		}
		ThreadHelper.waitUntilFinished( futures );
		System.out.println("Build " + n + " tables [ms]: " + ( System.currentTimeMillis() - start ) );
	}
}
