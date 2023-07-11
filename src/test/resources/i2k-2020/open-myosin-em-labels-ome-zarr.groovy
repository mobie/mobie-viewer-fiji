/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
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
