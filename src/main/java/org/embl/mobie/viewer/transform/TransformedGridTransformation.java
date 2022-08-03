/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
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
package org.embl.mobie.viewer.transform;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.Logger;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MultiThreading;
import org.embl.mobie.viewer.playground.SourceAffineTransformer;
import org.embl.mobie.viewer.source.Image;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public class TransformedGridTransformation< T > extends AbstractGridTransformation< T >
{
	// Serialization
	public List< List< String > > nestedSources;
	public List< List< String > > sourceNamesAfterTransform;
	public boolean centerAtOrigin = true;

	@Override
	public List< String > getTargetImageNames()
	{
		final ArrayList< String > allSources = new ArrayList<>();
		for ( List< String > sourcesAtGridPosition : nestedSources )
			allSources.addAll( sourcesAtGridPosition );
		return allSources;
	}

	private void transform( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter, double[] cellRealDimensions )
	{
		final int numGridPositions = nestedSources.size();

		final ArrayList< Future< ? > > futures = MultiThreading.getFutures();
		for ( int gridIndex = 0; gridIndex < numGridPositions; gridIndex++ )
		{
			int finalGridIndex = gridIndex;
			futures.add( MultiThreading.executorService.submit( () -> {
				if ( sourceNamesAfterTransform != null )
					translate( sourceNameToSourceAndConverter, nestedSources.get( finalGridIndex ), sourceNamesAfterTransform.get( finalGridIndex ), centerAtOrigin, cellRealDimensions[ 0 ] * positions.get( finalGridIndex )[ 0 ], cellRealDimensions[ 1 ] * positions.get( finalGridIndex )[ 1 ] );
				else
					translate( sourceNameToSourceAndConverter, nestedSources.get( finalGridIndex ), null, centerAtOrigin, cellRealDimensions[ 0 ] * positions.get( finalGridIndex )[ 0 ], cellRealDimensions[ 1 ] * positions.get( finalGridIndex )[ 1 ] );
			} ) );
		}
		MultiThreading.waitUntilFinished( futures );
	}

	public static void translate( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter, List< String > sourceNames, List< String > sourceNamesAfterTransform, boolean centerAtOrigin, double translationX, double translationY )
	{
		for ( String sourceName : sourceNames )
		{
			final SourceAndConverter< ? > sourceAndConverter = sourceNameToSourceAndConverter.get( sourceName );

			if ( sourceAndConverter == null )
			  continue;

			AffineTransform3D translationTransform = TransformHelper.createTranslationTransform3D( translationX, translationY, sourceAndConverter, centerAtOrigin );

			String transformedSourceName = sourceName;
			if ( sourceNamesAfterTransform != null )
				transformedSourceName = sourceNamesAfterTransform.get( sourceNames.indexOf( sourceName ) );

//			if ( sourceAndConverter instanceof LazySourceAndConverterAndTables )
//			{
//				// TODO: instead of modifying this one in place
//				//   we should better create a copy
//				//   maybe it would now even work with actually transforming the lazySpimSource
//				final LazySourceAndConverterAndTables lazySourceAndConverterAndTables = ( LazySourceAndConverterAndTables ) sourceAndConverter;
//				lazySourceAndConverterAndTables.setName( transformedSourceName );
//				final AffineTransform3D transform3D = new AffineTransform3D();
//				transform3D.preConcatenate( translationTransform ); // set by reference
//				sourceNameToSourceAndConverter.put( transformedSourceName, lazySourceAndConverterAndTables );
//			}
//			else
//			{
				final SourceAndConverter transformedSource = new SourceAffineTransformer( translationTransform, transformedSourceName, false ).apply( sourceAndConverter );

				sourceNameToSourceAndConverter.put( transformedSourceName, transformedSource );
//			}
		}
	}

}
