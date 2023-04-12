package org.embl.mobie.lib.table.saw;

import tech.tablesaw.aggregate.StringAggregateFunction;
import tech.tablesaw.api.StringColumn;

public abstract class Aggregators
{
	public static final StringAggregateFunction firstString =
		new StringAggregateFunction("First") {
			@Override
			public String summarize( StringColumn column)
			{
				if (column.size() > 0) return column.get(0);
				return "";
			}
		};
}
