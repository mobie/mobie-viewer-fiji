/**
 * Demonstrate loading of data from ome.zarr.s3 into BigDataViewer 
 * 
 * - lazy loading from s3
 * - multiscale layers
 * - label coloring [Ctrl L] to shuffle the LUT)
 * - interpolation [I], but not for labels
 */

import de.embl.cba.mobie.n5.zarr.*;
import de.embl.cba.mobie.n5.source.Sources;
import mpicbg.spim.data.SpimData;
import bdv.util.*;

//reader = new OMEZarrS3Reader( "https://play.minio.io:9000", "us-west-2", "i2k2020" );
//i2k = reader.read( "gif.zarr" );
//BdvFunctions.show( i2k );

N5OMEZarrImageLoader.debugLogging = true;
reader = new OMEZarrS3Reader( "https://s3.embl.de", "us-west-2", "i2k-2020" );
myosin = reader.read( "prospr-myosin.ome.zarr" );
myosinBdvSources = BdvFunctions.show( myosin );
emAndLabels = reader.read( "em-raw.ome.zarr" );
emAndLabelSources = BdvFunctions.show( emAndLabels, BdvOptions.options().addTo( myosinBdvSources.get( 0 ).getBdvHandle() ) );
Sources.showAsLabelMask( emAndLabelSources.get( 1 ) );