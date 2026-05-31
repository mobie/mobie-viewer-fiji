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
package org.embl.mobie.lib.data;

import ij.IJ;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.transform.GridType;
import tech.tablesaw.api.Table;

import java.util.Arrays;
import java.util.List;

/**
 * Builds dataset sources, displays and views from raw image / label / label-table
 * paths and adds them to a {@link Dataset}.
 *
 * Wraps {@link GridSourcesFromPathsCreator} (paths -> grid sources) and
 * {@link GridSourcesDataSetter} (grid sources -> Dataset) so callers — and tests —
 * can go from paths to a populated {@link Dataset} in one step, without touching
 * any UI code.
 */
public class GridImagesAndLabelsDataSetter
{
    private final List< String > imagePaths;
    private final List< String > labelPaths;
    private final List< String > labelTablePaths;
    private final String root;
    private final GridType grid;

    public GridImagesAndLabelsDataSetter(
            List< String > imagePaths,
            List< String > labelPaths,
            List< String > labelTablePaths,
            String root,
            GridType grid )
    {
        this.imagePaths = imagePaths;
        this.labelPaths = labelPaths;
        this.labelTablePaths = labelTablePaths;
        this.root = root;
        this.grid = grid;
    }

    public void addToDataset( Dataset dataset )
    {
        IJ.log( "Opening images: " + Arrays.toString( imagePaths.toArray() ) );
        IJ.log( "Opening labels: " + Arrays.toString( labelPaths.toArray() ) );
        IJ.log( "Opening tables: " + Arrays.toString( labelTablePaths.toArray() ) );

        final GridSourcesFromPathsCreator creator = new GridSourcesFromPathsCreator(
                imagePaths, labelPaths, labelTablePaths, root, grid );

        final List< ImageGridSources > imageSources = creator.getImageSources();
        final List< LabelGridSources > labelSources = creator.getLabelSources();
        final Table regionTable = creator.getRegionTable();

        new GridSourcesDataSetter( imageSources, labelSources, regionTable )
                .addDataAndDisplaysAndViews( dataset );
    }
}
