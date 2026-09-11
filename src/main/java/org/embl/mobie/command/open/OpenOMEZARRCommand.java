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
package org.embl.mobie.command.open;

import ij.IJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.imagedata.N5ImageData;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.data.CollectionDataSetter;
import org.embl.mobie.lib.io.FileImageSource;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.lib.table.columns.CollectionTableConstants;
import org.embl.mobie.lib.util.MoBIEHelper;
import org.embl.mobie.lib.util.ThreadHelper;
import org.jetbrains.annotations.NotNull;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_OPEN + "Open OME-Zarr...")
public class OpenOMEZARRCommand implements Command
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    @Parameter( label = "Container URI",
            description = "Local path or S3 address to an OME-Zarr container.\n" +
                    "All images within the container will be opened.\n" +
                    "If the path to an image contains \"labels\" it will be opened as a label mask.\n" +
                    "\nFor opening OME-Zarr HCS plates please instead use \"Open HCS Dataset...\""
    )
    public String containerUri;

    @Parameter( label = "( Labels URI )",
            description = "Optional. Local path or S3 address to an OME-Zarr label mask image.\n" +
                    "Use this to open additional labels that are not in the above container\n" +
                    "or if the path within the above container does not contain \"labels\".",
            required = false )
    public String labelsUri;

    @Parameter( label = "( Labels Table URI )",
            description = "Optional. Local path or S3 address to an table with label mask features.\n" +
                    "It will be assigned to the first label mask image that is opened above.",
            required = false )
    public String tableUri;

    @Parameter ( label = "( S3 Access Key )",
            description = "Optional. Access key for a protected S3 bucket.",
            persist = false,
            required = false )
    public String s3AccessKey;

    @Parameter ( label = "( S3 Secret Key )",
            description = "Optional. Secret key for a protected S3 bucket.",
            persist = false,
            required = false )
    public String s3SecretKey;

    @Override
    public void run()
    {
        final MoBIESettings settings = new MoBIESettings();

        final ArrayList< String > imageList = new ArrayList<>();
        final ArrayList< String > labelsList = new ArrayList<>();

        if ( MoBIEHelper.notNullOrEmpty( s3AccessKey ) )
            settings.s3AccessAndSecretKey( new String[]{ s3AccessKey, s3SecretKey } );

        if ( MoBIEHelper.notNullOrEmpty( containerUri ) )
        {
            IJ.log( "Analyzing " + containerUri + "..." );
            final N5ImageData< ? > n5ImageData = getN5ImageData();

            final int numDataSets = n5ImageData.getNumDatasets();
            IJ.log( "Found " + numDataSets + " datasets (channels counting as datasets)." );
            for ( int dataSetIndex = 0; dataSetIndex < numDataSets; dataSetIndex++ )
            {
                final String path = n5ImageData.getPath( dataSetIndex );
                if ( path.contains( "labels" ) && ( labelsUri == null || labelsUri.isEmpty() ) )
                    labelsList.add( containerUri + "=" + n5ImageData.getName( dataSetIndex ) + ";" + dataSetIndex );
                else
                    imageList.add( containerUri + "=" + n5ImageData.getName( dataSetIndex ) + ";" + dataSetIndex );
            }
        }

        if ( MoBIEHelper.notNullOrEmpty( labelsUri ) ) labelsList.add( labelsUri );

        final MoBIE moBIE = MoBIE.getInstance();
        final Set< String > sourceNames = new LinkedHashSet<>();
        final Set< String > viewNames = new LinkedHashSet<>();
        if ( moBIE != null )
        {
            sourceNames.addAll( moBIE.getDataset().sources().keySet() );
            viewNames.addAll( moBIE.getViews().keySet() );
        }

        final ArrayList< String > uniqueImages = uniquifySourceNames( imageList, sourceNames );
        final ArrayList< String > uniqueLabels = uniquifySourceNames( labelsList, sourceNames );

        final String viewBaseName = deriveViewBaseName( containerUri, uniqueImages, uniqueLabels );
        final String viewName = uniqueViewName( viewBaseName, viewNames );
        final Table table = createCollectionTable( uniqueImages, uniqueLabels, tableUri, viewName );

        try
        {
            if ( moBIE == null )
                new MoBIE( table, null, settings );
            else
                appendToRunningMoBIE( moBIE, table );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    private void appendToRunningMoBIE( final MoBIE moBIE, final Table table )
    {
        final Set< String > viewsBefore = new LinkedHashSet<>( moBIE.getViews().keySet() );
        new CollectionDataSetter( table, null ).addTableToDataset( moBIE.getDataset() );

        final Map< String, View > allViews = moBIE.getViews();
        final Map< String, View > addedViews = new LinkedHashMap<>();
        for ( Map.Entry< String, View > entry : allViews.entrySet() )
            if ( ! viewsBefore.contains( entry.getKey() ) )
                addedViews.put( entry.getKey(), entry.getValue() );

        if ( addedViews.isEmpty() )
        {
            IJ.log( "[WARN] No new MoBIE views were created while appending OME-Zarr data." );
            return;
        }

        moBIE.getUserInterface().addViews( addedViews );
        for ( String addedViewName : addedViews.keySet() )
            moBIE.getViewManager().show( addedViewName );
    }

    private Table createCollectionTable(
            final List< String > imageList,
            final List< String > labelsList,
            final String labelsTableUri,
            final String viewName )
    {
        final StringColumn uriColumn = StringColumn.create( CollectionTableConstants.URI[ 0 ] );
        final StringColumn nameColumn = StringColumn.create( CollectionTableConstants.NAME );
        final StringColumn typeColumn = StringColumn.create( CollectionTableConstants.TYPE );
        final IntColumn channelColumn = IntColumn.create( CollectionTableConstants.CHANNEL );
        final StringColumn viewColumn = StringColumn.create( CollectionTableConstants.VIEW );
        final StringColumn labelsTableColumn = StringColumn.create( CollectionTableConstants.LABELS_TABLE_URI[ 0 ] );
        final StringColumn contrastLimitsColumn = StringColumn.create( CollectionTableConstants.CONTRAST_LIMITS );
        final StringColumn formatColumn = StringColumn.create( CollectionTableConstants.FORMAT );

        int labelRowIndex = 0;

        for ( String entry : imageList )
        {
            final FileImageSource source = new FileImageSource( entry );
            uriColumn.append( source.path );
            nameColumn.append( source.name );
            typeColumn.append( CollectionTableConstants.INTENSITIES );
            channelColumn.append( source.channelIndex == null ? -1 : source.channelIndex );
            viewColumn.append( viewName );
            labelsTableColumn.appendMissing();
            contrastLimitsColumn.append( "auto" );
            formatColumn.append( ImageDataFormat.OmeZarr.name() );
        }

        for ( String entry : labelsList )
        {
            final FileImageSource source = new FileImageSource( entry );
            uriColumn.append( source.path );
            nameColumn.append( source.name );
            typeColumn.append( CollectionTableConstants.LABELS );
            channelColumn.append( source.channelIndex == null ? -1 : source.channelIndex );
            viewColumn.append( viewName );
            if ( labelRowIndex++ == 0 && MoBIEHelper.notNullOrEmpty( labelsTableUri ) )
                labelsTableColumn.append( labelsTableUri );
            else
                labelsTableColumn.appendMissing();
            contrastLimitsColumn.append( "auto" );
            formatColumn.append( ImageDataFormat.OmeZarr.name() );
        }

        return Table.create( "ome-zarr" ).addColumns(
                uriColumn,
                nameColumn,
                typeColumn,
                channelColumn,
                viewColumn,
                labelsTableColumn,
                contrastLimitsColumn,
                formatColumn );
    }

    private ArrayList< String > uniquifySourceNames( final List< String > paths, final Set< String > usedNames )
    {
        final ArrayList< String > unique = new ArrayList<>();
        for ( String path : paths )
        {
            final FileImageSource fileImageSource = new FileImageSource( path );
            final String uniqueName = uniqueName( fileImageSource.name, usedNames );
            final StringBuilder builder = new StringBuilder();
            builder.append( fileImageSource.path ).append( "=" ).append( uniqueName );
            if ( fileImageSource.channelIndex != null )
                builder.append( ";" ).append( fileImageSource.channelIndex );
            unique.add( builder.toString() );
        }
        return unique;
    }

    private String uniqueName( final String baseName, final Set< String > usedNames )
    {
        String candidate = baseName;
        int suffix = 2;
        while ( usedNames.contains( candidate ) )
            candidate = baseName + "-" + suffix++;
        usedNames.add( candidate );
        return candidate;
    }

    private String uniqueViewName( final String baseName, final Set< String > usedNames )
    {
        String candidate = baseName;
        int suffix = 2;
        while ( usedNames.contains( candidate ) )
            candidate = baseName + " (" + suffix++ + ")";
        usedNames.add( candidate );
        return candidate;
    }

    private String deriveViewBaseName(
            final String containerUri,
            final List< String > imageList,
            final List< String > labelsList )
    {
        if ( MoBIEHelper.notNullOrEmpty( containerUri ) )
        {
            String uri = containerUri;
            while ( uri.endsWith( "/" ) )
                uri = uri.substring( 0, uri.length() - 1 );

            if ( MoBIEHelper.notNullOrEmpty( uri ) )
            {
                final String fileName = IOHelper.getFileName( uri );
                if ( MoBIEHelper.notNullOrEmpty( fileName ) )
                    return stripZarrSuffix( fileName );
            }
        }

        for ( String path : Arrays.asList( imageList.isEmpty() ? null : imageList.get( 0 ), labelsList.isEmpty() ? null : labelsList.get( 0 ) ) )
        {
            if ( MoBIEHelper.notNullOrEmpty( path ) )
            {
                final String name = new FileImageSource( path ).name;
                if ( MoBIEHelper.notNullOrEmpty( name ) )
                    return name;
            }
        }

        return "OME-Zarr";
    }

    private String stripZarrSuffix( final String name )
    {
        if ( name.endsWith( ".ome.zarr" ) )
            return name.substring( 0, name.length() - ".ome.zarr".length() );
        if ( name.endsWith( ".zarr" ) )
            return name.substring( 0, name.length() - ".zarr".length() );
        return name;
    }

    @NotNull
    private N5ImageData< ? > getN5ImageData()
    {
        if ( MoBIEHelper.notNullOrEmpty( s3AccessKey ) )
            return new N5ImageData<>( containerUri, ThreadHelper.sharedQueue, new String[]{ s3AccessKey, s3SecretKey } );
        else
            return new N5ImageData<>( containerUri, ThreadHelper.sharedQueue );
    }
}

