package org.embl.mobie.lib;

import org.embl.mobie.MoBIE;
import org.embl.mobie.lib.annotation.AnnotatedSpot;
import org.embl.mobie.lib.image.SpotAnnotationImage;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.serialize.SpotDataSource;
import org.embl.mobie.lib.table.DefaultAnnData;
import org.embl.mobie.lib.table.TableDataFormat;
import org.embl.mobie.lib.table.saw.TableOpener;
import org.embl.mobie.lib.table.saw.TableSawAnnotatedSpot;
import org.embl.mobie.lib.table.saw.TableSawAnnotatedSpotCreator;
import org.embl.mobie.lib.table.saw.TableSawAnnotationCreator;
import org.embl.mobie.lib.table.saw.TableSawAnnotationTableModel;
import tech.tablesaw.api.Table;

public class SpotImageCreator
{
	private SpotAnnotationImage< AnnotatedSpot > spotAnnotationImage;

	public SpotImageCreator( SpotDataSource dataSource, MoBIE moBIE )
	{
		final SpotDataSource spotDataSource = dataSource;
		final StorageLocation tableLocation = moBIE.getTableLocation( spotDataSource.tableData );
		final TableDataFormat tableFormat = moBIE.getTableDataFormat( spotDataSource.tableData );

		Table table = TableOpener.open( tableLocation, tableFormat );

		final TableSawAnnotationCreator< TableSawAnnotatedSpot > annotationCreator = new TableSawAnnotatedSpotCreator( table );

		final TableSawAnnotationTableModel< AnnotatedSpot > tableModel = new TableSawAnnotationTableModel( dataSource.getName(), annotationCreator, tableLocation, tableFormat, table );

		final DefaultAnnData< AnnotatedSpot > spotAnnData = new DefaultAnnData<>( tableModel );

		spotAnnotationImage = new SpotAnnotationImage( spotDataSource.getName(), spotAnnData, 1.0, spotDataSource.boundingBoxMin, spotDataSource.boundingBoxMax );
	}

	public SpotAnnotationImage< AnnotatedSpot > create()
	{
		return spotAnnotationImage;
	}
}
