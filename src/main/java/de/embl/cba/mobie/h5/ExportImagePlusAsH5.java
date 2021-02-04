package de.embl.cba.mobie.h5;

import bdv.export.*;
import bdv.ij.export.imgloader.ImagePlusImgLoader;
import bdv.ij.util.PluginHelper;
import bdv.ij.util.ProgressWriterIJ;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.img.hdf5.Partition;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import de.embl.cba.mobie.n5.ExportImagePlusAsN5;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;

import java.awt.event.ItemEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.awt.AWTEvent;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.TextField;

import bdv.export.ExportMipmapInfo;
import bdv.export.ProgressWriter;
import bdv.export.ProposeMipmaps;
import bdv.export.SubTaskProgressWriter;
import bdv.export.WriteSequenceToHdf5;

import static de.embl.cba.mobie.utils.ExportUtils.*;

// copy of https://github.com/bigdataviewer/bigdataviewer_fiji/blob/master/src/main/java/bdv/ij/ExportImagePlusPlugIn.java
// with gui removed and some refactoring

public class ExportImagePlusAsH5 {

    public void export(ImagePlus imp, String xmlPath) {

        if (!isImageSuitable(imp)) {
            return;
        }

        FinalVoxelDimensions voxelSize = getVoxelSize(imp);
        FinalDimensions size = getSize(imp);

        // propose reasonable mipmap settings
        final ExportMipmapInfo autoMipmapSettings = ProposeMipmaps.proposeMipmaps(new BasicViewSetup(0, "", size, voxelSize));

        // use default parameters
        final H5Parameters params = getDefaultParameters(autoMipmapSettings, xmlPath);
        if (params == null)
            return;

        export(imp, params);
    }

    public void export(ImagePlus imp, H5Parameters params) {
        if (!isImageSuitable(imp)) {
            return;
        }

        final FinalVoxelDimensions voxelSize = getVoxelSize(imp);
        final FinalDimensions size = getSize(imp);

        final ProgressWriter progressWriter = new ProgressWriterIJ();
        progressWriter.out().println("starting export...");

        // create ImgLoader wrapping the image
        final ImagePlusImgLoader<?> imgLoader;
        switch (imp.getType()) {
            case ImagePlus.GRAY8:
                imgLoader = ImagePlusImgLoader.createGray8(imp, params.minMaxOption, params.rangeMin, params.rangeMax);
                break;
            case ImagePlus.GRAY16:
                imgLoader = ImagePlusImgLoader.createGray16(imp, params.minMaxOption, params.rangeMin, params.rangeMax);
                break;
            case ImagePlus.GRAY32:
            default:
                imgLoader = ImagePlusImgLoader.createGray32(imp, params.minMaxOption, params.rangeMin, params.rangeMax);
                break;
        }

        final int numTimepoints = imp.getNFrames();
        final int numSetups = imp.getNChannels();

        // create SourceTransform from the images calibration
        final AffineTransform3D sourceTransform = new AffineTransform3D();
        sourceTransform.set(voxelSize.dimension(0), 0, 0, 0, 0, voxelSize.dimension(1),
                0, 0, 0, 0, voxelSize.dimension(2), 0);

        // write hdf5
        final HashMap<Integer, BasicViewSetup> setups = new HashMap<>(numSetups);
        for (int s = 0; s < numSetups; ++s) {
            final BasicViewSetup setup = new BasicViewSetup(s, String.format("channel %d", s + 1), size, voxelSize);
            setup.setAttribute(new Channel(s + 1));
            setups.put(s, setup);
        }
        final ArrayList<TimePoint> timepoints = new ArrayList<>(numTimepoints);
        for (int t = 0; t < numTimepoints; ++t)
            timepoints.add(new TimePoint(t));
        final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal(new TimePoints(timepoints), setups, imgLoader, null);

        Map<Integer, ExportMipmapInfo> perSetupExportMipmapInfo;
        perSetupExportMipmapInfo = new HashMap<>();
        final ExportMipmapInfo mipmapInfo = new ExportMipmapInfo(params.resolutions, params.subdivisions);
        for (final BasicViewSetup setup : seq.getViewSetupsOrdered())
            perSetupExportMipmapInfo.put(setup.getId(), mipmapInfo);

        // LoopBackHeuristic:
        // - If saving more than 8x on pixel reads use the loopback image over
        //   original image
        // - For virtual stacks also consider the cache size that would be
        //   required for all original planes contributing to a "plane of
        //   blocks" at the current level. If this is more than 1/4 of
        //   available memory, use the loopback image.
        final boolean isVirtual = imp.getStack().isVirtual();
        final long planeSizeInBytes = imp.getWidth() * imp.getHeight() * imp.getBytesPerPixel();
        final long ijMaxMemory = IJ.maxMemory();
        final int numCellCreatorThreads = Math.max(1, PluginHelper.numThreads() - 1);
        final ExportScalePyramid.LoopbackHeuristic loopbackHeuristic = new ExportScalePyramid.LoopbackHeuristic() {
            @Override
            public boolean decide(final RandomAccessibleInterval<?> originalImg, final int[] factorsToOriginalImg, final int previousLevel, final int[] factorsToPreviousLevel, final int[] chunkSize) {
                if (previousLevel < 0)
                    return false;

                if (Intervals.numElements(factorsToOriginalImg) / Intervals.numElements(factorsToPreviousLevel) >= 8)
                    return true;

                if (isVirtual) {
                    final long requiredCacheSize = planeSizeInBytes * factorsToOriginalImg[2] * chunkSize[2];
                    if (requiredCacheSize > ijMaxMemory / 4)
                        return true;
                }

                return false;
            }
        };

        final ExportScalePyramid.AfterEachPlane afterEachPlane = new ExportScalePyramid.AfterEachPlane() {
            @Override
            public void afterEachPlane(final boolean usedLoopBack) {
                if (!usedLoopBack && isVirtual) {
                    final long free = Runtime.getRuntime().freeMemory();
                    final long total = Runtime.getRuntime().totalMemory();
                    final long max = Runtime.getRuntime().maxMemory();
                    final long actuallyFree = max - total + free;

                    if (actuallyFree < max / 2)
                        imgLoader.clearCache();
                }
            }

        };

        final ArrayList<Partition> partitions;
        if (params.split) {
            final String xmlFilename = params.seqFile.getAbsolutePath();
            final String basename = xmlFilename.endsWith(".xml") ? xmlFilename.substring(0, xmlFilename.length() - 4) : xmlFilename;
            partitions = Partition.split(timepoints, seq.getViewSetupsOrdered(), params.timepointsPerPartition, params.setupsPerPartition, basename);

            for (int i = 0; i < partitions.size(); ++i) {
                final Partition partition = partitions.get(i);
                final ProgressWriter p = new SubTaskProgressWriter(progressWriter, 0, 0.95 * i / partitions.size());
                WriteSequenceToHdf5.writeHdf5PartitionFile(seq, perSetupExportMipmapInfo, params.deflate, partition, loopbackHeuristic, afterEachPlane, numCellCreatorThreads, p);
            }
            WriteSequenceToHdf5.writeHdf5PartitionLinkFile(seq, perSetupExportMipmapInfo, partitions, params.hdf5File);
        } else {
            partitions = null;
            WriteSequenceToHdf5.writeHdf5File(seq, perSetupExportMipmapInfo, params.deflate, params.hdf5File, loopbackHeuristic, afterEachPlane, numCellCreatorThreads, new SubTaskProgressWriter(progressWriter, 0, 0.95));
        }

        // write xml sequence description
        final Hdf5ImageLoader hdf5Loader = new Hdf5ImageLoader(params.hdf5File, partitions, null, false);
        final SequenceDescriptionMinimal seqh5 = new SequenceDescriptionMinimal(seq, hdf5Loader);

        final ArrayList<ViewRegistration> registrations = new ArrayList<>();
        for (int t = 0; t < numTimepoints; ++t)
            for (int s = 0; s < numSetups; ++s)
                registrations.add(new ViewRegistration(t, s, sourceTransform));

        final File basePath = params.seqFile.getParentFile();
        final SpimDataMinimal spimData = new SpimDataMinimal(basePath, seqh5, new ViewRegistrations(registrations));

        try {
            new XmlIoSpimDataMinimal().save(spimData, params.seqFile.getAbsolutePath());
            progressWriter.setProgress(1.0);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        progressWriter.out().println("done");
    }

    public static class H5Parameters {
        final boolean setMipmapManual;

        final int[][] resolutions;

        final int[][] subdivisions;

        final File seqFile;

        final File hdf5File;

        final ImagePlusImgLoader.MinMaxOption minMaxOption;

        final double rangeMin;

        final double rangeMax;

        final boolean deflate;

        final boolean split;

        final int timepointsPerPartition;

        final int setupsPerPartition;

        public H5Parameters(
                final boolean setMipmapManual, final int[][] resolutions, final int[][] subdivisions,
                final File seqFile, final File hdf5File,
                final ImagePlusImgLoader.MinMaxOption minMaxOption, final double rangeMin, final double rangeMax, final boolean deflate,
                final boolean split, final int timepointsPerPartition, final int setupsPerPartition) {
            this.setMipmapManual = setMipmapManual;
            this.resolutions = resolutions;
            this.subdivisions = subdivisions;
            this.seqFile = seqFile;
            this.hdf5File = hdf5File;
            this.minMaxOption = minMaxOption;
            this.rangeMin = rangeMin;
            this.rangeMax = rangeMax;
            this.deflate = deflate;
            this.split = split;
            this.timepointsPerPartition = timepointsPerPartition;
            this.setupsPerPartition = setupsPerPartition;
        }
    }

    private File getH5File(String xmlPath) {
        final String hdf5Filename = xmlPath.substring(0, xmlPath.length() - 4) + ".h5";
        return new File(hdf5Filename);
    }

    static String lastManualSubsampling = "{ {1,1,1} }";

    static String lastManualChunkSizes = "{ {64,64,64} }";

    static int lastManualMinMaxChoice = 2;

    static double lastManualMin = 0;

    static double lastManualMax = 65535;

    static boolean lastManualSplit = false;

    static int lastManualTimepointsPerPartition = 0;

    static int lastManualSetupsPerPartition = 0;

    static boolean lastManualDeflate = true;

    public static double getLastManualMax() {
        return lastManualMax;
    }

    public static double getLastManualMin() {
        return lastManualMin;
    }

    public static int getLastManualMinMaxChoice() {
        return lastManualMinMaxChoice;
    }

    public static int getLastManualSetupsPerPartition() {
        return lastManualSetupsPerPartition;
    }

    public static int getLastManualTimepointsPerPartition() {
        return lastManualTimepointsPerPartition;
    }

    public static String getLastManualChunkSizes() {
        return lastManualChunkSizes;
    }

    public static String getLastManualSubsampling() {
        return lastManualSubsampling;
    }

    public static boolean getLastManualDeflate() {
        return lastManualDeflate;
    }

    public static boolean getLastManualSplit() {
        return lastManualSplit;
    }

    public H5Parameters getManualParameters(String subsamplingFactors, String h5ChunkSizes, int minMaxChoice,
                                            double min, double max, boolean splitHdf5, int timepointsPerPartition, int setupsPerPartition, boolean useDeflateCompression,
                                            String exportPath) {

        // parse mipmap resolutions and cell sizes
        final int[][] resolutions = PluginHelper.parseResolutionsString(subsamplingFactors);
        final int[][] subdivisions = PluginHelper.parseResolutionsString(h5ChunkSizes);
        if (!isResolutionsAndSubdivisionsSuitable(resolutions, subdivisions, subsamplingFactors, h5ChunkSizes)) {
            return null;
        }

        final ImagePlusImgLoader.MinMaxOption minMaxOption;
        if (minMaxChoice == 0)
            minMaxOption = ImagePlusImgLoader.MinMaxOption.TAKE_FROM_IMAGEPROCESSOR;
        else if (minMaxChoice == 1)
            minMaxOption = ImagePlusImgLoader.MinMaxOption.COMPUTE;
        else
            minMaxOption = ImagePlusImgLoader.MinMaxOption.SET;

        final File seqFile = getSeqFileFromPath(exportPath);
        if (seqFile == null) {
            return null;
        }

        File hdf5File = getH5File(exportPath);

        return new H5Parameters(true, resolutions, subdivisions, seqFile, hdf5File, minMaxOption,
                min, max, useDeflateCompression, splitHdf5, timepointsPerPartition, setupsPerPartition);
    }

    public H5Parameters getDefaultParameters(final ExportMipmapInfo autoMipmapSettings, String exportPath) {
        final String autoSubsampling = ProposeMipmaps.getArrayString(autoMipmapSettings.getExportResolutions());
        final String autoChunkSizes = ProposeMipmaps.getArrayString(autoMipmapSettings.getSubdivisions());

        return getManualParameters(autoSubsampling, autoChunkSizes, 2, 0, 65535,
                false, 0, 0, true, exportPath);
    }

    //  if(lastMinMaxChoice ==0) // use ImageJs...
    //
    // {
    //     lastMin = impMin;
    //     lastMax = impMax;
    // }

}