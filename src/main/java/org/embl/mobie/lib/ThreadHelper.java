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
package org.embl.mobie.lib;

import bdv.cache.SharedQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadHelper
{
	private static int N_IO_THREADS = Runtime.getRuntime().availableProcessors() - 1;

	private static final int N_THREADS = Runtime.getRuntime().availableProcessors() - 1;

	public static ExecutorService ioExecutorService = Executors.newFixedThreadPool( N_IO_THREADS );

	public static final SharedQueue sharedQueue = new SharedQueue( N_IO_THREADS );

	public static ExecutorService executorService = Executors.newFixedThreadPool( N_THREADS );

	public static ExecutorService stitchedImageExecutorService;
	static {
		// queue that only keep the latest requests.
		// use case: if a user zooms into a stitched image or adds a new channel
		// we don't want to wait until all the "old" cells are loaded that
		// have been requested already, but rather present the user with
		// the newly requested data.
		// idea taken from: https://stackoverflow.com/questions/53236911/executor-thread-pool-limit-queue-size-and-dequeue-oldest
		// fixes: https://github.com/mobie/mobie-viewer-fiji/issues/901
		RejectedExecutionHandler handler = new ThreadPoolExecutor.DiscardOldestPolicy();
		final int poolSize = ThreadHelper.getNumIoThreads();
		final LinkedBlockingQueue< Runnable > workQueue = new LinkedBlockingQueue<>( 2 * poolSize );
		stitchedImageExecutorService = new ThreadPoolExecutor(poolSize, poolSize,
				0L, TimeUnit.MILLISECONDS,
				workQueue,
				handler );
	}



	public static void resetIOThreads()
	{
		ioExecutorService.shutdownNow();
		ioExecutorService = Executors.newFixedThreadPool( N_IO_THREADS );
	}

	public static void setNumIoThreads( int numIoThreads )
	{
		N_IO_THREADS = numIoThreads;
		ioExecutorService = Executors.newFixedThreadPool( N_IO_THREADS );
	}

	public static int getNumIoThreads()
	{
		return N_IO_THREADS;
	}

	public static int getNumThreads()
	{
		return N_THREADS;
	}

	public static void waitUntilFinished( List< Future< ? > > futures )
	{
		for ( Future< ? > future : futures )
		{
			try
			{
				future.get();
			}
			catch ( InterruptedException e )
			{
				throw new RuntimeException( e );
			}
			catch ( ExecutionException e )
			{
				throw new RuntimeException( e );
			}
		}
	}

	public static ArrayList< Future< ? > > getFutures()
	{
		return new ArrayList<>();
	}
}
