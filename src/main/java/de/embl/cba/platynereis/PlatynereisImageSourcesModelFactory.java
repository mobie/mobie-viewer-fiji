package de.embl.cba.platynereis;

import de.embl.cba.tables.FileUtils;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PlatynereisImageSourcesModelFactory
{
	private final PlatynereisImageSourcesModel sourcesModel;

	public PlatynereisImageSourcesModelFactory( File directory )
	{
		List< File > imageFiles = getImageFiles( directory, ".*.xml" );

		sourcesModel = new PlatynereisImageSourcesModel();

		for ( File imageFile : imageFiles )
		{
			sourcesModel.addSource( imageFile );
		}

	}

	public PlatynereisImageSourcesModel getModel()
	{
		return sourcesModel;
	}

	public List< File > getImageFiles( File inputDirectory, String filePattern )
	{
		final List< File > fileList = FileUtils.getFileList( inputDirectory, filePattern );
		Collections.sort( fileList, new SortFilesIgnoreCase());
		return fileList;
	}


	public class SortFilesIgnoreCase implements Comparator<File>
	{
		public int compare( File o1, File o2 )
		{
			String s1 = o1.getName();
			String s2 = o2.getName();
			return s1.toLowerCase().compareTo(s2.toLowerCase());
		}
	}
}
