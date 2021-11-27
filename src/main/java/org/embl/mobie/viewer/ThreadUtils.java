package org.embl.mobie.viewer;

import bdv.util.volatiles.SharedQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ThreadUtils
{
	public static final int N_FETCHER_THREADS = Runtime.getRuntime().availableProcessors() - 1;
	public static final SharedQueue sharedQueue = new SharedQueue( N_FETCHER_THREADS );

	public static final int N_IO_THREADS = 8;
	public static final ExecutorService ioExecutorService = Executors.newFixedThreadPool( N_IO_THREADS );

	public static final int N_THREADS = Runtime.getRuntime().availableProcessors() - 1;;
	public static final ExecutorService executorService = Executors.newFixedThreadPool( N_THREADS );

	public static void waitUntilFinished( List< Future< ? > > futures )
	{
		for ( Future< ? > future : futures )
		{
			try
			{
				future.get();
			} catch ( InterruptedException e )
			{
				e.printStackTrace();
			} catch ( ExecutionException e )
			{
				e.printStackTrace();
			}
		}
	}

	public static ArrayList< Future< ? > > getFutures()
	{
		return new ArrayList<>();
	}
}
