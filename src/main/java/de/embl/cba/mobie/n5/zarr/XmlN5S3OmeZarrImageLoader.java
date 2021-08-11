package de.embl.cba.mobie.n5.zarr;

import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.ImgLoaderIo;
import mpicbg.spim.data.generic.sequence.XmlIoBasicImgLoader;
import org.jdom2.Element;

import java.io.File;

import static mpicbg.spim.data.XmlKeys.IMGLOADER_FORMAT_ATTRIBUTE_NAME;
@ImgLoaderIo(format = "bdv.ome.zarr.s3", type = N5S3OMEZarrImageLoader.class)
public class XmlN5S3OmeZarrImageLoader implements XmlIoBasicImgLoader<N5S3OMEZarrImageLoader>
{

    @Override
    public Element toXml( final N5S3OMEZarrImageLoader imgLoader, final File basePath) {
        final Element elem = new Element("ImageLoader");
        elem.setAttribute(IMGLOADER_FORMAT_ATTRIBUTE_NAME, "bdv.ome.zarr.s3");
        elem.setAttribute("version", "0.2");
        return elem;
    }

    @Override
    public N5S3OMEZarrImageLoader fromXml(Element elem, File basePath, AbstractSequenceDescription<?, ?, ?> sequenceDescription) {
        return null;
    }
}