package de.embl.cba.mobie.ui.viewer;

public class MoBIEOptions
{
	public final Values values = new Values();

	public enum ImageDataStorageModality
	{
		FileSystem,
		S3
	}

	public static MoBIEOptions options()
	{
		return new MoBIEOptions();
	}

	public MoBIEOptions dataset( String dataset )
	{
		this.values.dataset = dataset;
		return this;
	}

	public MoBIEOptions gitProjectBranch( String gitBranch )
	{
		this.values.projectBranch = gitBranch;
		return this;
	}

	public MoBIEOptions imageDataStorageModality( ImageDataStorageModality imageDataStorageModality )
	{
		this.values.imageDataStorageModality = imageDataStorageModality;
		return this;
	}

	public MoBIEOptions imageDataLocation( String imageDataLocation )
	{
		this.values.imageDataLocation = imageDataLocation;
		return this;
	}

	public MoBIEOptions tableDataLocation( String tableDataLocation )
	{
		this.values.tableDataLocation = tableDataLocation;
		return this;
	}

	public MoBIEOptions gitTablesBranch( String tableDataBranch )
	{
		this.values.tableDataBranch = tableDataBranch;
		return this;
	}

	public MoBIEOptions pulicationURL( String publicationURL )
	{
		this.values.publicationURL = publicationURL;
		return this;
	}

	public static class Values
	{
		private String publicationURL;
		private String dataset;
		private String projectBranch = "master"; // project and images
		private String tableDataBranch;
		private ImageDataStorageModality imageDataStorageModality = ImageDataStorageModality.S3;
		private String imageDataLocation;
		private String tableDataLocation;

		public String getDataset()
		{
			return dataset;
		}
		public String getProjectBranch()
		{
			return projectBranch;
		}
		public ImageDataStorageModality getImageDataStorageModality() { return imageDataStorageModality; }

		public String getImageDataLocation()
		{
			return imageDataLocation;
		}

		public String getTableDataLocation()
		{
			return tableDataLocation;
		}

		public String getTableDataBranch()
		{
			return tableDataBranch != null ? tableDataBranch : projectBranch;
		}

		public String getPublicationURL()
		{
			return publicationURL;
		}
	}
}
