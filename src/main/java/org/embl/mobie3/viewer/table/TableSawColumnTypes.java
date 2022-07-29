package org.embl.mobie3.viewer.table;

import tech.tablesaw.api.ColumnType;

import java.util.HashMap;
import java.util.Map;

public abstract class TableSawColumnTypes
{
	public static Map< ColumnType, Class< ? > > typeToClass;
	static {
		typeToClass = new HashMap<>();
		typeToClass.put( ColumnType.STRING, String.class );
		typeToClass.put( ColumnType.DOUBLE, Double.class );
		typeToClass.put( ColumnType.INTEGER, Integer.class );
		typeToClass.put( ColumnType.SHORT, Short.class );
		typeToClass.put( ColumnType.LONG, Long.class );
		//...
	}
}
