/**
 * Demonstrate loading of data from ome.zarr.s3 into BigDataViewer 
 * 
 * - lazy loading from s3
 * - multiscale layers
 * - label coloring [Ctrl L] to shuffle the LUT)
 * - interpolation [I], but not for labels
 *
 * Run this script in Fiji
 *
 * [ File > New > Script ... ]
 * [ Language > Groovy ]
 *
 * or, even interactive
 *
 * [ Plugins > Scripting > Script Interpreter ]
 * [ Groovy ]
 *
 * Note: it seems that one has to re-paste the import statement for the Sources, not sure why....
 *
 */

import de.embl.cba.mobie.n5.zarr.*;
import de.embl.cba.mobie.n5.source.Sources;
import bdv.util.*;

N5OMEZarrImageLoader.logChunkLoading = true;
reader = new OMEZarrS3Reader( "https://s3.embl.de", "us-west-2", "i2k-2020" );
myosin = reader.readKey( "prospr-myosin.ome.zarr" );
myosinBdvSources = BdvFunctions.show( myosin );
emAndLabels = reader.readKey( "em-raw.ome.zarr" );
emAndLabelSources = BdvFunctions.show( emAndLabels, BdvOptions.options().addTo( myosinBdvSources.get( 0 ).getBdvHandle() ) );
Sources.showAsLabelMask( emAndLabelSources.get( 1 ) );

//Sources.viewAsHyperstack( emAndLabelSources.get( 0 ), 4 );
