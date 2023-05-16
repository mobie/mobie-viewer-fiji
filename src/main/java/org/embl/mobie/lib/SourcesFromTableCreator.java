package org.embl.mobie.lib;

import org.embl.mobie.lib.io.TableImageSource;
import org.embl.mobie.lib.table.saw.TableOpener;
import org.embl.mobie.lib.transform.GridType;
import tech.tablesaw.api.Table;

import java.util.ArrayList;
import java.util.List;

public class SourcesFromTableCreator
{
	private List< ImageFiles > imageFileSources;
	private List< LabelFiles > labelSources;

	public SourcesFromTableCreator( String tablePath, List< String > imageColumns, List< String > labelColumns, String root, GridType gridType )
	{
		final Table table = TableOpener.openDelimitedTextFile( tablePath );

		imageFileSources = new ArrayList<>();

		for ( String image : imageColumns )
		{
			final TableImageSource tableImageSource = new TableImageSource( image );
			imageFileSources.add( new ImageFiles( tableImageSource.name, table, tableImageSource.columnName, tableImageSource.channelIndex, root, gridType ) );
		}

		labelSources = new ArrayList<>();
		// see https://github.com/mobie/mobie-viewer-fiji/issues/1038
		final String firstLabel = labelColumns.get( 0 );
		for ( String label : labelColumns )
		{
			final TableImageSource tableImageSource = new TableImageSource( label );
			labelSources.add( new LabelFiles( tableImageSource.name, table, tableImageSource.columnName, tableImageSource.channelIndex, root, gridType, label.equals( firstLabel ) ) );
		}
	}

	public List< ImageFiles > getImageSources()
	{
		return imageFileSources;
	}

	public List< LabelFiles > getLabelSources()
	{
		return labelSources;
	}
}
