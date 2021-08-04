/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
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
package de.embl.cba.mobie.n5.zarr;

import bdv.AbstractViewerSetupImgLoader;
import bdv.ViewerImgLoader;
import bdv.cache.CacheControl;
import bdv.img.cache.SimpleCacheArrayLoader;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.util.ConstantRandomAccessible;
import bdv.util.MipmapTransforms;
import bdv.util.volatiles.SharedQueue;
import com.amazonaws.SdkClientException;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.*;
import net.imglib2.*;
import net.imglib2.cache.queue.BlockingFetchQueues;
import net.imglib2.cache.queue.FetcherThreads;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.volatiles.array.*;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.CellImg;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.*;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.*;
import net.imglib2.util.Cast;
import net.imglib2.view.Views;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5CellLoader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

public class N5OMEZarrImageLoader implements ViewerImgLoader, MultiResolutionImgLoader {
	private static final int C = 3;
	private static final int T = 4;
	protected final N5Reader n5;
	protected AbstractSequenceDescription<?, ?, ?> seq;
	protected ViewRegistrations viewRegistrations;
	public static boolean logChunkLoading = false;
	private static boolean is5D = false;
	private static boolean is4DC = false;
	private static boolean is4DT = false;
	private static boolean is2D = false;

	/**
	 * Maps setup id to {@link SetupImgLoader}.
	 */
	private final Map<Integer, SetupImgLoader> setupImgLoaders = new HashMap<>();

	private volatile boolean isOpen = false;
	private FetcherThreads fetchers;
	private VolatileGlobalCellCache cache;
	private final Map<Integer, String> setupToPathname = new HashMap<>();
	private final Map<Integer, Multiscale> setupToMultiscale = new HashMap<>();
	private final Map<Integer, DatasetAttributes> setupToAttributes = new HashMap<>();
	private final Map<Integer, Integer> setupToChannel = new HashMap<>();
	private int sequenceTimepoints = 0;
	private HashMap<String, Integer> axesMap = new HashMap<>();
	private BlockingFetchQueues< Callable< ? > > queue;


	/**
	 * The sequenceDescription and viewRegistrations are known already, typically read from xml.
	 *
	 * @param n5Reader            reader
	 * @param sequenceDescription
	 */
	@Deprecated
	public N5OMEZarrImageLoader(N5Reader n5Reader, AbstractSequenceDescription<?, ?, ?> sequenceDescription) {
		this.n5 = n5Reader;
		this.seq = sequenceDescription; // TODO: it is better to fetch from within Zarr
	}

	public N5OMEZarrImageLoader(N5Reader n5Reader, HashMap<String, Integer> axesMap) {
		this.n5 = n5Reader;
		this.axesMap = axesMap;
		fetchSequenceDescriptionAndViewRegistrations();
	}

	public N5OMEZarrImageLoader(N5Reader n5Reader, HashMap<String, Integer> axesMap, BlockingFetchQueues< Callable< ? > > queue ) {
		this.n5 = n5Reader;
		this.axesMap = axesMap;
		this.queue = queue;
		fetchSequenceDescriptionAndViewRegistrations();
	}

	private void fetchSequenceDescriptionAndViewRegistrations() {
		try {
			initSetups();

			ArrayList<ViewSetup> viewSetups = new ArrayList<>();
			ArrayList<ViewRegistration> viewRegistrationList = new ArrayList<>();

			int numSetups = setupToMultiscale.size();
			for (int setupId = 0; setupId < numSetups; setupId++) {
				ViewSetup viewSetup = createViewSetup(setupId);
				int setupTimepoints = 1;
				is5D = false;

				if (setupToAttributes.get(setupId).getNumDimensions() > 4) {
					setupTimepoints = (int) setupToAttributes.get(setupId).getDimensions()[T];
					is5D = true;
				}

				if (setupToAttributes.get(setupId).getNumDimensions() == 4 && axesMap.containsKey("t")) {
					setupTimepoints = (int) setupToAttributes.get(setupId).getDimensions()[3];
					is4DT = true;
				}

				sequenceTimepoints = Math.max(setupTimepoints, sequenceTimepoints);
				viewSetups.add(viewSetup);
				viewRegistrationList.addAll(createViewRegistrations(setupId, setupTimepoints));
			}

			viewRegistrations = new ViewRegistrations(viewRegistrationList);

			seq = new SequenceDescription(new TimePoints(createTimePoints(sequenceTimepoints)), viewSetups);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@NotNull
	private ArrayList<TimePoint> createTimePoints(int sequenceTimepoints) {
		ArrayList<TimePoint> timePoints = new ArrayList<>();
		for (int t = 0; t < sequenceTimepoints; t++) {
			timePoints.add(new TimePoint(t));
		}
		return timePoints;
	}

	@NotNull
	private void initSetups() throws IOException {
		int setupId = -1;

		Multiscale multiscale = getMultiscale(""); // returns multiscales[ 0 ]
		DatasetAttributes attributes = getDatasetAttributes(multiscale.datasets[0].path);
		long nC = 1;
		if (attributes.getNumDimensions() > 4) {
			nC = attributes.getDimensions()[C];
		}
		if (attributes.getNumDimensions() == 4 && axesMap.containsKey("c") && !axesMap.containsKey("t")) {
			nC = attributes.getDimensions()[C];
			is4DC = true;
		}
		if (attributes.getNumDimensions() == 4 && axesMap.containsKey("c") && axesMap.containsKey("t")) {
			nC = attributes.getDimensions()[2];
			is4DC = true;
		}
		if (attributes.getNumDimensions() == 2 && axesMap.containsKey("y") && axesMap.containsKey("x")) {
			is2D = true;
		}

		for (int c = 0; c < nC; c++) {
			// each channel is one setup
			setupId++;
			setupToChannel.put(setupId, c);

			// all channels have the same multiscale and attributes
			setupToMultiscale.put(setupId, multiscale);
			setupToAttributes.put(setupId, attributes);
			setupToPathname.put(setupId, "");
		}

		List<String> labels = n5.getAttribute("labels", "labels", List.class);
		if (labels != null) {
			for (String label : labels) {
				setupId++;
				setupToChannel.put(setupId, 0); // TODO: https://github.com/ome/ngff/issues/19
				String pathName = "labels/" + label;
				multiscale = getMultiscale(pathName);
				attributes = getDatasetAttributes(pathName + "/" + multiscale.datasets[0].path);

				setupToMultiscale.put(setupId, multiscale);
				setupToAttributes.put(setupId, attributes);
				setupToPathname.put(setupId, pathName);
			}
		}
	}

	/**
	 * The dataType, number of channels and number of timepoints are stored
	 * in the different pyramid levels (datasets).
	 * According to the spec all datasets must be indentical in that sense
	 * and we thus fetch this information from level 0.
	 * <p>
	 * In addition, level 0 contains the information about the size of the full resolution image.
	 *
	 * @param pathName
	 * @return
	 * @throws IOException
	 */
	private DatasetAttributes getDatasetAttributes(String pathName) throws IOException {
		return n5.getDatasetAttributes(pathName);
	}

	/**
	 * The primary use case for multiple multiscales at the moment (the one listed in the spec)
	 * is multiple different downsamplings.
	 * A base image with two multiscales each with a different scale or a different method.
	 * <p>
	 * I don't know a good logic right now how to deal with different pyramids,
	 * thus I just fetch the first one, i.e. multiscales[ 0 ].
	 * <p>
	 * ??? There's no need for the two multiscales to have the same base though.
	 * ??? So it would also allow you to just have two pyramids (in our jargon) in the same zgroup.
	 *
	 * @param pathName
	 * @return
	 * @throws IOException
	 */
	private Multiscale getMultiscale(String pathName) throws IOException {
		final String key = "multiscales";
		Multiscale[] multiscales = n5.getAttribute(pathName, key, Multiscale[].class);
		if (multiscales == null) {
			String location = "";
			if (n5 instanceof N5S3ZarrReader) {
				final N5S3ZarrReader s3ZarrReader = (N5S3ZarrReader) n5;
				s3ZarrReader.setDimensionSeparator("/");
				location += "service endpoint: " + s3ZarrReader.getServiceEndpoint();
				location += "; bucket: " + s3ZarrReader.getBucketName();
				location += "; container path: " + s3ZarrReader.getContainerPath();
				location += "; path: " + pathName;
				location += "; attribute: " + key;
			}
			throw new UnsupportedOperationException("Could not find multiscales at " + location);
		}
		return multiscales[0];
	}

	public AbstractSequenceDescription<?, ?, ?> getSequenceDescription() {
		//open();
		seq.setImgLoader(Cast.unchecked(this));
		return seq;
	}

	public ViewRegistrations getViewRegistrations() {
		return viewRegistrations;
	}

	private void open() {
		if (!isOpen) {
			synchronized (this) {
				if (isOpen)
					return;

				try {
					int maxNumLevels = 0;
					final List<? extends BasicViewSetup> setups = seq.getViewSetupsOrdered();
					for (final BasicViewSetup setup : setups) {
						final int setupId = setup.getId();
						final SetupImgLoader setupImgLoader = createSetupImgLoader(setupId);
						setupImgLoaders.put(setupId, setupImgLoader);
						maxNumLevels = Math.max(maxNumLevels, setupImgLoader.numMipmapLevels());
					}

					if ( queue == null )
					{
						final int numFetcherThreads = Math.max( 1, Runtime.getRuntime().availableProcessors() );
						queue = new BlockingFetchQueues<>( maxNumLevels, numFetcherThreads );
						fetchers = new FetcherThreads( queue, numFetcherThreads);
					}
					cache = new VolatileGlobalCellCache( queue );
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

				isOpen = true;
			}
		}
	}

	private static class Multiscale {
		String name;
		double[][] scales;
		Transform transform;
		Dataset[] datasets;
	}

	private static class Dataset {
		String path;
	}

	private static class Transform {
		String[] axes;
		double[] scale;
		double[] translate;
		String[] units;
	}

	@NotNull
	private ArrayList<ViewRegistration> createViewRegistrations(int setupId, int setupTimepoints) {
		Multiscale multiscale = setupToMultiscale.get(setupId);
		AffineTransform3D transform = new AffineTransform3D();

		try {
			double[] scale = multiscale.transform.scale;
			transform.scale(scale[0], scale[1], scale[2]);
		} catch (Exception e) {
			System.out.println("No scale given" + e);
		}

		ArrayList<ViewRegistration> viewRegistrations = new ArrayList<>();
		for (int t = 0; t < setupTimepoints; t++)
			viewRegistrations.add(new ViewRegistration(t, setupId, transform));

		return viewRegistrations;
	}

	private ViewSetup createViewSetup(int setupId) {
		final DatasetAttributes attributes = setupToAttributes.get(setupId);
		FinalDimensions dimensions = new FinalDimensions(attributes.getDimensions());
		Multiscale multiscale = setupToMultiscale.get(setupId);
		VoxelDimensions voxelDimensions = readVoxelDimensions(multiscale);
		Tile tile = new Tile(0);

		Channel channel;
		if (setupToPathname.get(setupId).contains("labels"))
			channel = new Channel(setupToChannel.get(setupId), "labels");
		else
			channel = new Channel(setupToChannel.get(setupId));

		Angle angle = new Angle(0);
		Illumination illumination = new Illumination(0);
		String name = readName(multiscale, setupId);
		//if ( setupToPathname.get( setupId ).contains( "labels" ))
		//	viewSetup.setAttribute( new ImageType( ImageType.Type.IntensityImage ) );
		return new ViewSetup(setupId, name, dimensions, voxelDimensions, tile, channel, angle, illumination);
	}

	private String readName(Multiscale multiscale, int setupId) {
		if (multiscale.name != null)
			return multiscale.name;
		else
			return "image " + setupId;
	}

	@NotNull
	private VoxelDimensions readVoxelDimensions(Multiscale multiscale) {
		try {
			return new FinalVoxelDimensions(multiscale.transform.units[0], multiscale.transform.scale);
		} catch (Exception e) {
			return new DefaultVoxelDimensions(3);
		}
	}

	/**
	 * Clear the cache. Images that were obtained from
	 * this loader before {@link #close()} will stop working. Requesting images
	 * after {@link #close()} will cause the n5 to be reopened (with a
	 * new cache).
	 */
	public void close() {
		if (isOpen) {
			synchronized (this) {
				if (!isOpen)
					return;
				if ( fetchers != null )
					fetchers.shutdown();
				cache.clearCache();
				isOpen = false;
			}
		}
	}

	@Override
	public SetupImgLoader getSetupImgLoader(final int setupId) {
		open();
		return setupImgLoaders.get(setupId);
	}

	private <T extends NativeType<T>, V extends Volatile<T> & NativeType<V>> SetupImgLoader<T, V> createSetupImgLoader(final int setupId) throws IOException {
		switch (setupToAttributes.get(setupId).getDataType()) {
			case UINT8:
				return Cast.unchecked(new SetupImgLoader<>(setupId, new UnsignedByteType(), new VolatileUnsignedByteType()));
			case UINT16:
				return Cast.unchecked(new SetupImgLoader<>(setupId, new UnsignedShortType(), new VolatileUnsignedShortType()));
			case UINT32:
				return Cast.unchecked(new SetupImgLoader<>(setupId, new UnsignedIntType(), new VolatileUnsignedIntType()));
			case UINT64:
				return Cast.unchecked(new SetupImgLoader<>(setupId, new UnsignedLongType(), new VolatileUnsignedLongType()));
			case INT8:
				return Cast.unchecked(new SetupImgLoader<>(setupId, new ByteType(), new VolatileByteType()));
			case INT16:
				return Cast.unchecked(new SetupImgLoader<>(setupId, new ShortType(), new VolatileShortType()));
			case INT32:
				return Cast.unchecked(new SetupImgLoader<>(setupId, new IntType(), new VolatileIntType()));
			case INT64:
				return Cast.unchecked(new SetupImgLoader<>(setupId, new LongType(), new VolatileLongType()));
			case FLOAT32:
				return Cast.unchecked(new SetupImgLoader<>(setupId, new FloatType(), new VolatileFloatType()));
			case FLOAT64:
				return Cast.unchecked(new SetupImgLoader<>(setupId, new DoubleType(), new VolatileDoubleType()));
		}
		return null;
	}

	@Override
	public CacheControl getCacheControl() {
		open();
		return cache;
	}

	private class SetupImgLoader<T extends NativeType<T>, V extends Volatile<T> & NativeType<V>>
			extends AbstractViewerSetupImgLoader<T, V>
			implements MultiResolutionSetupImgLoader<T> {
		private final int setupId;

		private final double[][] mipmapResolutions;

		private final AffineTransform3D[] mipmapTransforms;

		public SetupImgLoader(final int setupId, final T type, final V volatileType) throws IOException {
			super(type, volatileType);
			this.setupId = setupId;
			mipmapResolutions = readMipmapResolutions();
			mipmapTransforms = new AffineTransform3D[mipmapResolutions.length];
			for (int level = 0; level < mipmapResolutions.length; level++) {
				mipmapTransforms[level] = MipmapTransforms.getMipmapTransformDefault(mipmapResolutions[level]);
			}
		}

		/**
		 * @return
		 * @throws IOException
		 */
		private double[][] readMipmapResolutions() throws IOException {
			Multiscale multiscale = setupToMultiscale.get(setupId);
			double[][] mipmapResolutions = new double[multiscale.datasets.length][];

			try {
				System.arraycopy(multiscale.scales, 0, mipmapResolutions, 0, mipmapResolutions.length);
			} catch (Exception e) {

				//Try to fix 2D problem
				long[] dimensionsOfLevel0 = getDatasetAttributes(getPathName(setupId, 0)).getDimensions();
				mipmapResolutions[0] = new double[]{1.0, 1.0, 1.0};

				for (int level = 1; level < mipmapResolutions.length; level++) {
					long[] dimensions = getDatasetAttributes(getPathName(setupId, level)).getDimensions();
					mipmapResolutions[level] = new double[3];
					if (dimensions.length < 3 && dimensionsOfLevel0.length < 3) {
						for (int d = 0; d < 2; d++) {
							mipmapResolutions[level][d] = 1.0 * dimensionsOfLevel0[d] / dimensions[d];
						}
						mipmapResolutions[level][2] = 1.0;
					} else {
						mipmapResolutions[level] = new double[3];
						for (int d = 0; d < 3; d++) {
							mipmapResolutions[level][d] = 1.0 * dimensionsOfLevel0[d] / dimensions[d];
						}
					}
				}
			}

			return mipmapResolutions;
		}

		@Override
		public RandomAccessibleInterval<V> getVolatileImage(final int timepointId, final int level, final ImgLoaderHint... hints) {
			return prepareCachedImage(timepointId, level, LoadingStrategy.BUDGETED, volatileType);
		}

		@Override
		public RandomAccessibleInterval<T> getImage(final int timepointId, final int level, final ImgLoaderHint... hints) {
			return prepareCachedImage(timepointId, level, LoadingStrategy.BLOCKING, type);
		}

		@Override
		public Dimensions getImageSize(final int timepointId, final int level) {
			final String pathName = getPathName(setupId, level);
			try {
				final DatasetAttributes attributes = getDatasetAttributes(pathName);
				return new FinalDimensions(attributes.getDimensions());
			} catch (Exception e) {
				throw new RuntimeException("Could not read from " + pathName);
			}
		}

		@NotNull
		public String getPathName(int setupId, int level) {
			return setupToPathname.get(setupId) + "/" + setupToMultiscale.get(this.setupId).datasets[level].path;
		}

		@Override
		public double[][] getMipmapResolutions() {
			return mipmapResolutions;
		}

		@Override
		public AffineTransform3D[] getMipmapTransforms() {
			return mipmapTransforms;
		}

		@Override
		public int numMipmapLevels() {
			return mipmapResolutions.length;
		}

		@Override
		public VoxelDimensions getVoxelSize(final int timepointId) {
			return null;
		}

		/**
		 * Create a {@link CellImg} backed by the cache.
		 */
		private <T extends NativeType<T>> RandomAccessibleInterval<T> prepareCachedImage(final int timepointId, final int level, final LoadingStrategy loadingStrategy, final T type) {
			try {
				final String pathName = getPathName(setupId, level);
				final DatasetAttributes attributes = getDatasetAttributes(pathName);

				if (logChunkLoading) {
					System.out.println("Preparing image " + pathName + " of data type " + attributes.getDataType());
				}
				long[] dimensions = getDimensions(attributes);
				final int[] cellDimensions = getBlockSize(attributes);
				final CellGrid grid = new CellGrid(dimensions, cellDimensions);

				final int priority = numMipmapLevels() - 1 - level;
				final CacheHints cacheHints = new CacheHints(loadingStrategy, priority, false);

				final SimpleCacheArrayLoader<?> loader = createCacheArrayLoader(n5, pathName, setupToChannel.get(setupId), timepointId, grid);
				return cache.createImg(grid, timepointId, setupId, level, cacheHints, loader, type);
			} catch (IOException e) {
				System.err.printf(
						"image data for timepoint %d setup %d level %d could not be found.%n",
						timepointId, setupId, level);
				return Views.interval(
						new ConstantRandomAccessible<>(type.createVariable(), 3),
						new FinalInterval(1, 1, 1));
			}
		}
	}

	private long[] getDimensions(DatasetAttributes attributes) {
		if (!axesMap.isEmpty()) {
			if ((axesMap.size() == 2) || (axesMap.size() == 4 && axesMap.containsKey("c") && axesMap.containsKey("t"))) {
				return fillDimensions(attributes);
			}
		}
		return Arrays.stream(attributes.getDimensions()).limit(3).toArray();
	}

	private long[] fillDimensions(DatasetAttributes attributes) {
		long[] tmp = new long[3];
		tmp[0] = Arrays.stream(attributes.getDimensions()).toArray()[0];
		tmp[1] = Arrays.stream(attributes.getDimensions()).toArray()[1];
		tmp[2] = 1;
		return tmp;
	}

	private int[] getBlockSize(DatasetAttributes attributes) {
		if (!axesMap.isEmpty()) {
			if ((axesMap.size() == 2) || (axesMap.size() == 4 && axesMap.containsKey("c") && axesMap.containsKey("t"))) {
				return fillBlockSize(attributes);
			}
		}
		return Arrays.stream(attributes.getBlockSize()).limit(3).toArray();
	}

	private int[] fillBlockSize(DatasetAttributes attributes) {
		int[] tmp = new int[3];
		tmp[0] = Arrays.stream(attributes.getBlockSize()).toArray()[0];
		tmp[1] = Arrays.stream(attributes.getBlockSize()).toArray()[1];
		tmp[2] = 1;
		return tmp;
	}

	private static class ArrayCreator<A, T extends NativeType<T>> {
		private final CellGrid cellGrid;
		private final DataType dataType;
		private final BiConsumer<ArrayImg<T, ?>, DataBlock<?>> copyFromBlock;

		public ArrayCreator(CellGrid cellGrid, DataType dataType) {
			this.cellGrid = cellGrid;
			this.dataType = dataType;
			this.copyFromBlock = N5CellLoader.createCopy(dataType);
		}

		public A createArray(DataBlock<?> dataBlock, long[] gridPosition) {
			long[] cellDims = getCellDims(gridPosition);
			int n = (int) (cellDims[0] * cellDims[1] * cellDims[2]);

			if (is2D)
				cellDims = Arrays.stream(cellDims).limit(2).toArray();

			switch (dataType) {
				case UINT8:
				case INT8:
					byte[] bytes = new byte[n];
					copyFromBlock.accept(Cast.unchecked(ArrayImgs.bytes(bytes, cellDims)), dataBlock);
					return (A) new VolatileByteArray(bytes, true);
				case UINT16:
				case INT16:
					short[] shorts = new short[n];
					copyFromBlock.accept(Cast.unchecked(ArrayImgs.shorts(shorts, cellDims)), dataBlock);
					return (A) new VolatileShortArray(shorts, true);
				case UINT32:
				case INT32:
					int[] ints = new int[n];
					copyFromBlock.accept(Cast.unchecked(ArrayImgs.ints(ints, cellDims)), dataBlock);
					return (A) new VolatileIntArray(ints, true);
				case UINT64:
				case INT64:
					long[] longs = new long[n];
					copyFromBlock.accept(Cast.unchecked(ArrayImgs.longs(longs, cellDims)), dataBlock);
					return (A) new VolatileLongArray(longs, true);
				case FLOAT32:
					float[] floats = new float[n];
					copyFromBlock.accept(Cast.unchecked(ArrayImgs.floats(floats, cellDims)), dataBlock);
					return (A) new VolatileFloatArray(floats, true);
				case FLOAT64:
					double[] doubles = new double[n];
					copyFromBlock.accept(Cast.unchecked(ArrayImgs.doubles(doubles, cellDims)), dataBlock);
					return (A) new VolatileDoubleArray(doubles, true);
				default:
					throw new IllegalArgumentException();
			}
		}

		public A createEmptyArray(long[] gridPosition) {
			long[] cellDims = getCellDims(gridPosition);
			int n = (int) (cellDims[0] * cellDims[1] * cellDims[2]);
			switch (dataType) {
				case UINT8:
				case INT8:
					return Cast.unchecked(new VolatileByteArray(new byte[n], true));
				case UINT16:
				case INT16:
					return Cast.unchecked(new VolatileShortArray(new short[n], true));
				case UINT32:
				case INT32:
					return Cast.unchecked(new VolatileIntArray(new int[n], true));
				case UINT64:
				case INT64:
					return Cast.unchecked(new VolatileLongArray(new long[n], true));
				case FLOAT32:
					return Cast.unchecked(new VolatileFloatArray(new float[n], true));
				case FLOAT64:
					return Cast.unchecked(new VolatileDoubleArray(new double[n], true));
				default:
					throw new IllegalArgumentException();
			}
		}

		private long[] getCellDims(long[] gridPosition) {
			long[] cellMin = new long[3];
			int[] cellDims = new int[3];
			if (is4DC && !is4DT) {
				cellMin = new long[4];
				cellDims = new int[4];
				cellDims[3] = 1; // channel
			}
			if (is4DT && !is4DC) {
				cellMin = new long[4];
				cellDims = new int[4];
				cellDims[3] = 1; // channel
			}
			if (is4DT && is4DC) {
				cellMin = new long[4];
				cellDims = new int[4];
				cellDims[2] = 1; // channel
				cellDims[3] = 1; // timepoint
			}
			if (is5D) {
				cellMin = new long[5];
				cellDims = new int[5];
				cellDims[3] = 1; // channel
				cellDims[4] = 1; // timepoint
			}

			cellGrid.getCellDimensions(gridPosition, cellMin, cellDims);
			return Arrays.stream(cellDims).mapToLong(i -> i).toArray(); // casting to long for creating ArrayImgs.*
		}
	}

	private static class N5OMEZarrCacheArrayLoader<A> implements SimpleCacheArrayLoader<A> {
		private final N5Reader n5;
		private final String pathName;
		private final int channel;
		private final int timepoint;
		private final DatasetAttributes attributes;
		private final ArrayCreator<A, ?> arrayCreator;

		N5OMEZarrCacheArrayLoader(final N5Reader n5, final String pathName, final int channel, final int timepoint, final DatasetAttributes attributes, CellGrid grid) {
			this.n5 = n5;
			this.pathName = pathName; // includes the level
			this.channel = channel;
			this.timepoint = timepoint;
			this.attributes = attributes;
			this.arrayCreator = new ArrayCreator<>(grid, attributes.getDataType());
		}

		@Override
		public A loadArray(final long[] gridPosition) throws IOException {
			DataBlock<?> block = null;

			long[] dataBlockIndices = toDataBlockIndices(gridPosition);

			long start = 0;
			if (logChunkLoading) {
				start = System.currentTimeMillis();
				System.out.println(pathName + " " + Arrays.toString(dataBlockIndices) + " ...");
			}

			try {
				block = n5.readBlock(pathName, attributes, dataBlockIndices);
			} catch (SdkClientException e) {
				System.err.println(e); // this happens sometimes, not sure yet why...
			}

			if (logChunkLoading) {
				if (block != null)
					System.out.println(pathName + " " + Arrays.toString(dataBlockIndices) + " fetched " + block.getNumElements() + " voxels in " + (System.currentTimeMillis() - start) + " ms.");
				else
					System.out.println(pathName + " " + Arrays.toString(dataBlockIndices) + " is missing, returning zeros.");
			}

			if (block == null) {
				return arrayCreator.createEmptyArray(gridPosition);
			} else {
				return arrayCreator.createArray(block, gridPosition);
			}
		}

		private long[] toDataBlockIndices(long[] gridPosition) {
			long[] dataBlockIndices = gridPosition;

			if (is2D) {
				dataBlockIndices = new long[2];
				System.arraycopy(gridPosition, 0, dataBlockIndices, 0, 2);
			}

			if (is4DC && is4DT) {
				dataBlockIndices = new long[4];
				System.arraycopy(gridPosition, 0, dataBlockIndices, 0, 2);
				dataBlockIndices[2] = channel;
				dataBlockIndices[3] = timepoint;
			}

			if (is5D) {
				dataBlockIndices = new long[5];
				System.arraycopy(gridPosition, 0, dataBlockIndices, 0, 3);
				dataBlockIndices[3] = channel;
				dataBlockIndices[4] = timepoint;
			}

			if (is4DC && !is4DT) {
				dataBlockIndices = new long[4];
				System.arraycopy(gridPosition, 0, dataBlockIndices, 0, 3);
				dataBlockIndices[3] = channel;
			}

			if (is4DT && !is4DC) {
				dataBlockIndices = new long[4];
				System.arraycopy(gridPosition, 0, dataBlockIndices, 0, 3);
				dataBlockIndices[3] = timepoint;
			}

			if (dataBlockIndices == null)
				throw new RuntimeException("Could not determine the data block to be loaded.");

			return dataBlockIndices;
		}
	}

	private static SimpleCacheArrayLoader<?> createCacheArrayLoader(final N5Reader n5, final String pathName, int channel, int timepointId, CellGrid grid) throws IOException {
		final DatasetAttributes attributes = n5.getDatasetAttributes(pathName);
		return new N5OMEZarrCacheArrayLoader<>(n5, pathName, channel, timepointId, attributes, grid);
	}
}

