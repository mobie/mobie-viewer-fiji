package de.embl.cba.platynereis.utils.ui;

import ij.gui.GenericDialog;

import java.util.ArrayList;

public class DatasetsDialog
{
	public String showDialog( ArrayList< String > datasets )
	{
		final String[] array = asArray( datasets );
		final GenericDialog gd = new GenericDialog( "Data set" );
		gd.addChoice( "Data Version", array, array[ array.length - 1 ] );
		gd.showDialog();
		if ( gd.wasCanceled() ) return null;
		else return gd.getNextChoice();
	}

	private String[] asArray( ArrayList< String > versions )
	{
		final String[] versionsArray = new String[ versions.size() ];
		for ( int i = 0; i < versionsArray.length; i++ )
			versionsArray[ i ] = versions.get( i );
		return versionsArray;
	}
}
