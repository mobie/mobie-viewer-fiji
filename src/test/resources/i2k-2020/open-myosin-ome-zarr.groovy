import de.embl.cba.mobie.n5.zarr.*;
import de.embl.cba.mobie.n5.source.Sources;
import mpicbg.spim.data.SpimData;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;

//N5OMEZarrImageLoader.debugLogging = true;
reader = new OMEZarrS3Reader( "https://s3.embl.de", "us-west-2", "i2k-2020" );
myosin = reader.read( "prospr-myosin.ome.zarr" );
BdvFunctions.show( myosin );