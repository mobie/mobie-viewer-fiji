package org.embl.mobie.lib;

import org.embl.mobie.MoBIE;
import org.embl.mobie.lib.annotation.AnnotatedSegment;
import org.embl.mobie.lib.annotation.DefaultAnnotationAdapter;
import org.embl.mobie.lib.annotation.LazyAnnotatedSegmentAdapter;
import org.embl.mobie.lib.image.AnnotationLabelImage;
import org.embl.mobie.lib.image.DefaultAnnotationLabelImage;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.serialize.SegmentationDataSource;
import org.embl.mobie.lib.table.DefaultAnnData;
import org.embl.mobie.lib.table.LazyAnnotatedSegmentTableModel;
import org.embl.mobie.lib.table.TableDataFormat;
import org.embl.mobie.lib.table.columns.SegmentColumnNames;
import org.embl.mobie.lib.table.saw.TableOpener;
import org.embl.mobie.lib.table.saw.TableSawAnnotatedSegment;
import org.embl.mobie.lib.table.saw.TableSawAnnotatedSegmentCreator;
import org.embl.mobie.lib.table.saw.TableSawAnnotationTableModel;
import tech.tablesaw.api.Table;

public class AnnotatedLabelImageCreator
{
	private AnnotationLabelImage< TableSawAnnotatedSegment > annotatedLabelImage;

	public AnnotatedLabelImageCreator( MoBIE moBIE, SegmentationDataSource dataSource, Image< ? > image )
	{
		if ( dataSource.tableData != null )
		{
			//System.out.println(dataSource.getName() + ": initialising.." );

			final StorageLocation tableLocation = moBIE.getTableLocation( dataSource.tableData );
			final TableDataFormat tableFormat =  moBIE.getTableDataFormat( dataSource.tableData );

			//System.out.println( dataSource.getName() + ": pre-init: " + dataSource.preInit() );

			Table table = dataSource.preInit() ?
					TableOpener.open( tableLocation, tableFormat ) : null;

			//SegmentColumnNames segmentColumnNames = table != null ?
			//		TableDataFormat.getSegmentColumnNames( table.columnNames() ) : null;

			final TableSawAnnotatedSegmentCreator annotationCreator = new TableSawAnnotatedSegmentCreator( table );

			final TableSawAnnotationTableModel< TableSawAnnotatedSegment >  tableModel = new TableSawAnnotationTableModel( dataSource.getName(), annotationCreator, tableLocation, tableFormat, table );

			final DefaultAnnData< TableSawAnnotatedSegment > annData = new DefaultAnnData<>( tableModel );

			final DefaultAnnotationAdapter< TableSawAnnotatedSegment > annotationAdapter = new DefaultAnnotationAdapter( annData );

			annotatedLabelImage = new DefaultAnnotationLabelImage( image, annData, annotationAdapter );
		}
		else
		{
			// label image without annotation table
			final LazyAnnotatedSegmentTableModel tableModel = new LazyAnnotatedSegmentTableModel( image.getName() );
			final DefaultAnnData< AnnotatedSegment > annData = new DefaultAnnData<>( tableModel );
			final LazyAnnotatedSegmentAdapter segmentAdapter = new LazyAnnotatedSegmentAdapter( image.getName(), tableModel );
			annotatedLabelImage = new DefaultAnnotationLabelImage( image, annData, segmentAdapter );
		}
	}

	public AnnotationLabelImage< ? > create()
	{
		return annotatedLabelImage;
	}
}
