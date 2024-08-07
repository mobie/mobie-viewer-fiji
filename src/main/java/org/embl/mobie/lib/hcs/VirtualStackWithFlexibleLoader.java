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
package org.embl.mobie.lib.hcs;

import ij.*;
import ij.io.Opener;
import ij.plugin.FolderOpener;
import ij.process.*;
import ij.util.Tools;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.util.IOHelper;

import java.awt.*;
import java.awt.image.ColorModel;
import java.io.File;
import java.util.Properties;


public class VirtualStackWithFlexibleLoader extends ImageStack
{
    private static final int INITIAL_SIZE = 100;
    private ImageDataFormat imageDataFormat;
    private String path;
    private int nSlices;
    private String[] names;
    private String[] labels;
    private int bitDepth;
    private int delay;
    private Properties properties;
    private boolean generateData;
    private int[] indexes;  // used to translate non-CZT hyperstack slice numbers


    public VirtualStackWithFlexibleLoader() { }

    public VirtualStackWithFlexibleLoader( int width, int height) {
        super(width, height);
    }

    public VirtualStackWithFlexibleLoader( int width, int height, ColorModel cm, String path, ImageDataFormat imageDataFormat ) {
        super(width, height, cm);
        this.imageDataFormat = imageDataFormat;
        path = IJ.addSeparator(path);
        this.path = path;
        names = new String[INITIAL_SIZE];
        labels = new String[INITIAL_SIZE];
    }

    public VirtualStackWithFlexibleLoader( int width, int height, int slices) {
        this(width, height, slices, "8-bit");
    }

    public VirtualStackWithFlexibleLoader( int width, int height, int slices, String options) {
        super(width, height, null);
        nSlices = slices;
        int depth = 8;
        if (options.contains("16-bit")) depth=16;
        if (options.contains("RGB")) depth=24;
        if (options.contains("32-bit")) depth=32;
        if (options.contains("delay")) delay=250;
        this.generateData = options.contains("fill");
        this.bitDepth = depth;
    }

    public void addSlice(String fileName) {
        if (fileName==null)
            throw new IllegalArgumentException("'fileName' is null!");
        if (fileName.startsWith("."))
            return;
        if (names==null)
            throw new IllegalArgumentException("VirtualStack(w,h,cm,path) constructor not used");
        nSlices++;
        if (nSlices==names.length) {
            String[] tmp = new String[nSlices*2];
            System.arraycopy(names, 0, tmp, 0, nSlices);
            names = tmp;
            tmp = new String[nSlices*2];
            System.arraycopy(labels, 0, tmp, 0, nSlices);
            labels = tmp;
        }
        names[nSlices-1] = fileName;
    }

    public void addSlice(String sliceLabel, Object pixels) {
    }

    public void addSlice(String sliceLabel, ImageProcessor ip) {
    }

    public void addSlice(String sliceLabel, ImageProcessor ip, int n) {
    }

    public void deleteSlice(int n) {
        if (nSlices==0)
            return;
        if (n<1 || n>nSlices)
            throw new IllegalArgumentException("Argument out of range: "+n);
        for (int i=n; i<nSlices; i++)
            names[i-1] = names[i];
        names[nSlices-1] = null;
        nSlices--;
    }

    public void deleteLastSlice() {
        int n = size();
        if (n>0)
            deleteSlice(n);
    }

    public Object getPixels(int n) {
        ImageProcessor ip = getProcessor(n);
        if (ip!=null)
            return ip.getPixels();
        else
            return null;
    }

    public void setPixels(Object pixels, int n) {
    }

    public ImageProcessor getProcessor(int n) {
        if (path==null) {  //Help>Examples?JavaScript>Terabyte VirtualStack
            ImageProcessor ip = null;
            int w=getWidth(), h=getHeight();
            switch (bitDepth) {
                case 8: ip = new ByteProcessor(w,h); break;
                case 16: ip = new ShortProcessor(w,h); break;
                case 24: ip = new ColorProcessor(w,h); break;
                case 32: ip = new FloatProcessor(w,h); break;
            }
            String hlabel = null;
            if (generateData) {
                int value = 0;
                ImagePlus img = WindowManager.getCurrentImage();
                if (img!=null && img.getStackSize()==nSlices)
                    value = img.getCurrentSlice()-1;
                if (bitDepth==16)
                    value *= 256;
                if (bitDepth!=32) {
                    for (int i=0; i<ip.getPixelCount(); i++)
                        ip.set(i,value++);
                }
                if (img!=null && img.isHyperStack()) {
                    int[] pos = img.convertIndexToPosition(n);
                    hlabel = pos[0]+" "+pos[1]+" "+pos[2]+" "+n;
                }
            }
            label(ip, hlabel!=null?hlabel:""+n, Color.white);
            if (delay>0)
                IJ.wait(delay);
            return ip;
        }
        n = translate(n);  // update n for hyperstacks not in the default CZT order
        String path = getFileName( n );
        // Open the image
        ImagePlus imp;
        if ( imageDataFormat.equals( ImageDataFormat.BioFormats ) )
        {
            imp = IOHelper.openWithBioFormats(path, 0);
            imp.setTitle( names[n-1] );
        }
        else if ( imageDataFormat.equals( ImageDataFormat.Tiff ))
        {
            imp = IOHelper.openTiffFromFile(path);
        }
        else
        {
            throw new RuntimeException( "Opening files with " + imageDataFormat + " into a VirtualStack is currently not supported");
        }
        ImageProcessor ip = null;
        int depthThisImage = 0;
        if (imp!=null) {
            int w = imp.getWidth();
            int h = imp.getHeight();
            int type = imp.getType();
            ColorModel cm = imp.getProcessor().getColorModel();
            String info = (String)imp.getProperty("Info");
            if (info!=null) {
                if ( FolderOpener.useInfo(info))
                    labels[n-1] = info;
            } else {
                String sliceLabel = imp.getStack().getSliceLabel(1);
                if (FolderOpener.useInfo(sliceLabel))
                    labels[n-1] = "Label: "+sliceLabel;
            }
            depthThisImage = imp.getBitDepth();
            ip = imp.getProcessor();
            ip.setOverlay(imp.getOverlay());
            properties = imp.getProperty("FHT")!=null?imp.getProperties():null;
        } else {
            File f = new File( this.path, names[n-1]);
            String msg = f.exists()?"Error opening ":"File not found: ";
            ip = new ByteProcessor(getWidth(), getHeight());
            ip.invert();
            label(ip, msg+names[n-1], Color.black);
            depthThisImage = 8;
        }
        if (depthThisImage!=bitDepth) {
            switch (bitDepth) {
                case 8: ip=ip.convertToByte(true); break;
                case 16: ip=ip.convertToShort(true); break;
                case 24:  ip=ip.convertToRGB(); break;
                case 32: ip=ip.convertToFloat(); break;
            }
        }
        if (ip.getWidth()!=getWidth() || ip.getHeight()!=getHeight()) {
            ImageProcessor ip2 = ip.createProcessor(getWidth(), getHeight());
            ip2.insert(ip, 0, 0);
            ip = ip2;
        }
        if (cTable!=null)
            ip.setCalibrationTable(cTable);
        return ip;
    }

    private void label(ImageProcessor ip, String msg, Color color) {
        int size = getHeight()/20;
        if (size<9) size=9;
        Font font = new Font("Helvetica", Font.PLAIN, size);
        ip.setFont(font);
        ip.setAntialiasedText(true);
        ip.setColor(color);
        ip.drawString(msg, size, size*2);
    }

    public int saveChanges(int n) {
        return -1;
    }

    public int size() {
        return getSize();
    }

    public int getSize() {
        return nSlices;
    }

    public String getSliceLabel(int n) {
        if (labels==null)
            return null;
        String label = labels[n-1];
        if (label==null)
            return names[n-1];
        else {
            if (label.startsWith("Label: "))  // slice label
                return label.substring(7,label.length());
            else
                return names[n-1]+"\n"+label;
        }
    }

    public Object[] getImageArray() {
        return null;
    }

    public void setSliceLabel(String label, int n) {
    }

    public boolean isVirtual() {
        return true;
    }

    public void trim() {
    }

    public String getDirectory() {
        return IJ.addSeparator(path);
    }

    public String getFileName(int n) {
        return names[n-1];
    }

    public void setBitDepth(int bitDepth) {
        this.bitDepth = bitDepth;
    }

    public int getBitDepth() {
        return bitDepth;
    }

    public ImageStack sortDicom(String[] strings, String[] info, int maxDigits) {
        int n = size();
        String[] names2 = new String[n];
        for (int i=0; i<n; i++)
            names2[i] = names[i];
        for (int i=0; i<n; i++) {
            int slice = (int) Tools.parseDouble(strings[i].substring(strings[i].length()-maxDigits), 0.0);
            if (slice==0) return null;
            names[i] = names2[slice-1];
            labels[i] = info[slice-1];
        }
        return this;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setIndexes(int[] indexes) {
        this.indexes = indexes;
    }

    public int translate(int n) {
        int n2 = (indexes!=null&&indexes.length==getSize()) ? indexes[n-1]+1 : n;
        //IJ.log("translate: "+n+" "+n2+" "+getSize()+" "+(indexes!=null?indexes.length:null));
        return n2;
    }

    public void reduce(int factor) {
        if (factor<2 || nSlices/factor<1 || names==null)
            return;
        nSlices = nSlices/factor;
        for (int i=0; i<nSlices; i++) {
            names[i] = names[i*factor];
            labels[i] = labels[i*factor];
        }
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp!=null) {
            imp.setSlice(1);
            imp.updateAndRepaintWindow();
        }
    }

}

