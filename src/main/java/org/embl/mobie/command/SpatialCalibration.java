package org.embl.mobie.command;

import ij.IJ;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import org.embl.mobie.MoBIESettings;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.table.columns.PixelSizeColumns;
import org.embl.mobie.lib.table.saw.TableOpener;
import tech.tablesaw.api.Table;

public enum SpatialCalibration
{
    FromImage,
    FromTable,
    UsePixelUnits;

    // TODO: it is not nice that the tablePath is required here even though it only is needed
    //       if this.equals( FromTable )
    public void setVoxelDimensions( MoBIESettings settings, String tablePath )
    {
        if ( this.equals( FromTable ) )
        {
            // Often the path is a regex, thus we need to resolve the actual paths
            String resolvedTablePath = IOHelper.getPaths( tablePath, 999 ).get( 0 );

            try
            {
                Table rows = TableOpener.openDelimitedTextFile( resolvedTablePath );

                if ( rows.columnNames().contains( PixelSizeColumns.PIXEL_SIZE ) )
                {
                    FinalVoxelDimensions voxelDimensions = new FinalVoxelDimensions(
                            rows.stringColumn( PixelSizeColumns.PIXEL_UNIT ).get( 0 ),
                            rows.doubleColumn( PixelSizeColumns.PIXEL_SIZE ).get( 0 ),
                            rows.doubleColumn( PixelSizeColumns.PIXEL_SIZE ).get( 0 ),
                            rows.doubleColumn( PixelSizeColumns.PIXEL_SIZE ).get( 0 )
                    );
                    settings.setVoxelDimensions( voxelDimensions );
                }
                else
                {
                    FinalVoxelDimensions voxelDimensions = new FinalVoxelDimensions(
                            rows.stringColumn( PixelSizeColumns.PIXEL_UNIT ).get( 0 ),
                            rows.doubleColumn( PixelSizeColumns.PIXEL_SIZE_X ).get( 0 ),
                            rows.doubleColumn( PixelSizeColumns.PIXEL_SIZE_Y ).get( 0 ),
                            rows.doubleColumn( PixelSizeColumns.PIXEL_SIZE_Z ).get( 0 )
                    );
                    settings.setVoxelDimensions( voxelDimensions );
                }
            }
            catch ( Exception e )
            {
                IJ.log("[ERROR] Could not read spatial calibration from table: " + tablePath );
                IJ.log("Please check that the tables contain the following columns:");
                IJ.log( PixelSizeColumns.PIXEL_SIZE + " and " + PixelSizeColumns.PIXEL_UNIT );
                IJ.log( "or" );
                IJ.log( PixelSizeColumns.PIXEL_SIZE_X + ", " +
                        PixelSizeColumns.PIXEL_SIZE_Y + ", " +
                        PixelSizeColumns.PIXEL_SIZE_Z + ", " +
                        " and " + PixelSizeColumns.PIXEL_UNIT );
                throw new RuntimeException( e );
            }
        }
        else if ( this.equals( UsePixelUnits ) )
        {
            FinalVoxelDimensions voxelDimensions = new FinalVoxelDimensions(
                    "pixel", 1.0, 1.0, 1.0 );
            settings.setVoxelDimensions( voxelDimensions );
        }
    }
}
