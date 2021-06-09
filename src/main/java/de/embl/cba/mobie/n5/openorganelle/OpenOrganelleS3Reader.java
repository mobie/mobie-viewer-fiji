package de.embl.cba.mobie.n5.openorganelle;

import bdv.util.BdvFunctions;
import de.embl.cba.mobie.n5.S3Reader;
import mpicbg.spim.data.SpimData;
import net.imglib2.util.Cast;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class OpenOrganelleS3Reader extends S3Reader {
    public OpenOrganelleS3Reader(String serviceEndpoint, String signingRegion, String bucketName) {
        super(serviceEndpoint, signingRegion, bucketName);
    }

    public SpimData readKey(String key) throws IOException {
        OpenOrganelleN5S3ImageLoader imageLoader = new OpenOrganelleN5S3ImageLoader(serviceEndpoint, signingRegion, bucketName, key);
        return new SpimData(null, Cast.unchecked(imageLoader.getSequenceDescription()), imageLoader.getViewRegistrations());
    }

    public static SpimData readURL(String url) throws IOException {
        final String[] split = url.split("/");
        String serviceEndpoint = Arrays.stream(split).limit(3).collect(Collectors.joining("/"));
        String signingRegion = "us-west-2";
        String bucketName = split[3];
        final String key = Arrays.stream(split).skip(4).collect(Collectors.joining("/"));
        final OpenOrganelleS3Reader reader = new OpenOrganelleS3Reader(serviceEndpoint, signingRegion, bucketName);
        return reader.readKey(key);
    }

    public static void main(String[] args) throws IOException {
        showHela();
    }

    public static void showHela() throws IOException {
        OpenOrganelleS3Reader reader = new OpenOrganelleS3Reader(
                "https://janelia-cosem.s3.amazonaws.com",
                "us-west-2",
                "jrc_hela-2");
        OpenOrganelleS3Reader.setLogChunkLoading(true);
        SpimData image = reader.readKey("jrc_hela-2.n5/em/fibsem-uint16");
        BdvFunctions.show(image);
    }
}
