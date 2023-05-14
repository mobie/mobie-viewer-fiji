package org.embl.mobie.lib;

import org.embl.mobie.lib.annotation.AnnotatedSegment;
import org.embl.mobie.lib.annotation.DefaultAnnotationAdapter;
import org.embl.mobie.lib.annotation.LazyAnnotatedSegmentAdapter;
import org.embl.mobie.lib.image.AnnotatedLabelImage;
import org.embl.mobie.lib.image.DefaultAnnotatedLabelImage;
import org.embl.mobie.lib.serialize.SegmentationDataSource;
import org.embl.mobie.lib.table.DefaultAnnData;
import org.embl.mobie.lib.table.LazyAnnotatedSegmentTableModel;
import org.embl.mobie.lib.table.saw.TableSawAnnotatedSegment;
import org.embl.mobie.lib.table.saw.TableSawAnnotationTableModel;

public class AnnotatedLabelImageCreator
{
	private AnnotatedLabelImage< TableSawAnnotatedSegment > annotatedLabelImage;

	public AnnotatedLabelImageCreator( SegmentationDataSource segmentationDataSource )
	{
		if ( segmentationDataSource.tableData != null )
		{
			// label image representing annotated segments
			TableSawAnnotationTableModel< TableSawAnnotatedSegment > tableModel = createTableModel( segmentationDataSource );
			final DefaultAnnData< TableSawAnnotatedSegment > annData = new DefaultAnnData<>( tableModel );
			final DefaultAnnotationAdapter< TableSawAnnotatedSegment > annotationAdapter = new DefaultAnnotationAdapter( annData );
			annotatedLabelImage = new DefaultAnnotatedLabelImage( image, annData, annotationAdapter );
		}
		else
		{
			// label image representing segments without annotation table
			final LazyAnnotatedSegmentTableModel tableModel = new LazyAnnotatedSegmentTableModel( image.getName() );
			final DefaultAnnData< AnnotatedSegment > annData = new DefaultAnnData<>( tableModel );
			final LazyAnnotatedSegmentAdapter segmentAdapter = new LazyAnnotatedSegmentAdapter( image.getName(), tableModel );
			final DefaultAnnotatedLabelImage< ? > annotatedLabelImage = new DefaultAnnotatedLabelImage( image, annData, segmentAdapter );
			DataStore.putImage( annotatedLabelImage );
		}
	}

	public AnnotatedLabelImage< ? > getAnnotatedLabelImage()
	{
		return annotatedLabelImage;
	}
}
