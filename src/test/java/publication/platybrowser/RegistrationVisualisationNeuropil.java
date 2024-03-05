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
package publication.platybrowser;

import net.imagej.ImageJ;

public class RegistrationVisualisationNeuropil
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		// final MoBIE moBIE = new MoBIE(
		// 		"/Volumes/arendt/EM_6dpf_segmentation/platy-browser-data/data",
		// 		"https://git.embl.de/tischer/platy-browser-tables/raw/dev/data" );
		//
		// final SourcesDisplayManager sourcesDisplayManager = moBIE.getSourcesDisplayManager();
		//
		// sourcesDisplayManager.show( "prospr-6dpf-1-whole-segmented-aridane-Neuropil" );
		// sourcesDisplayManager.show( "prospr-6dpf-1-whole-Glt1-MED" );
		//
		// sourcesDisplayManager.setSourceColor( "prospr-6dpf-1-whole-segmented-aridane-Neuropil", new Color( 255,0,255,255) );
		// sourcesDisplayManager.setSourceColor( "prospr-6dpf-1-whole-Glt1-MED", new Color( 0,255,0,255) );
		//
		//
		// // OVERVIEW
		//
		// BdvLocationChanger.moveToLocation( moBIE.getSourcesDisplayManager().getBdv(), new BdvLocation( "View: (-0.7478710175280877, -0.6676436522042725, 2.086953854660592, 291.5334749475883, 1.4642245659643176, -1.7927905904117467, -0.048824338475451055, 337.95348110873744, 1.6300830832316395, 1.3040664665853117, 1.0013367511280067, -516.0999606550258)" ) );
		//
		// // TODO: get this via keyboard shortcut
		// UniverseUtils.setVolumeView("center =  1.0 0.0 0.0 122.88 0.0 1.0 0.0 122.88 0.0 0.0 1.0 140.40001 0.0 0.0 0.0 1.0\n" +
		// 		"translate =  1.0 0.0 0.0 0.117842264 0.0 1.0 0.0 0.15685147 0.0 0.0 1.0 -0.52159584 0.0 0.0 0.0 1.0\n" +
		// 		"rotate =  -0.22049984 -0.6480566 -0.72897357 0.0 -0.22329058 0.76105946 -0.60904 0.0 0.94948465 0.028479699 -0.31251845 0.0 0.0 0.0 0.0 1.0\n" +
		// 		"zoom =  1.0 0.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 0.0 1.0 358.10345 0.0 0.0 0.0 1.0\n" +
		// 		"animate =  1.0 0.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 0.0 1.0\n", moBIE.getSourcesDisplayManager().getUniverse()
		// );

		// DETAIL ZOOM
		// View: (-5.457887183190528, 6.892455295688354, -0.09128822640025808, 243.5120593221493, -3.0323636942634073, -2.2962312393397, 7.926846697708436, 869.0433860416233, 6.19023779584073, 4.952190236672585, 3.8025746460164447, -2153.870206057355)
	}
}
