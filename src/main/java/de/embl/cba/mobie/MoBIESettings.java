package de.embl.cba.mobie;

import com.google.gson.annotations.SerializedName;

public class MoBIESettings
{
	public final Values values = new Values();

	public enum ImageDataFormat
	{
		@SerializedName("bdv.n5")
		BdvN5,
		@SerializedName("bdv.n5.s3")
		BdvN5S3,
		@SerializedName("openOrganelle")
		OpenOrganelle;

		@Override
		public String toString()
		{
			switch ( this )
			{
				case BdvN5:
					return "bdv.n5";
				case BdvN5S3:
					return "bdv.n5.s3";
				case OpenOrganelle:
					return "openOrganelle";
				default:
					throw new UnsupportedOperationException("Unknown image file format: " + this );
			}
		}
	}

	public static MoBIESettings settings()
	{
		return new MoBIESettings();
	}

	public MoBIESettings projectLocation( String projectLocation )
	{
		this.values.projectLocation = projectLocation;
		return this;
	}

	public MoBIESettings dataset( String dataset )
	{
		this.values.dataset = dataset;
		return this;
	}

	public MoBIESettings gitProjectBranch( String gitBranch )
	{
		this.values.projectBranch = gitBranch;
		return this;
	}

	public MoBIESettings imageDataFormat( ImageDataFormat imageDataFormat )
	{
		this.values.imageDataFormat = imageDataFormat;
		return this;
	}

	public MoBIESettings imageDataLocation( String imageDataLocation )
	{
		this.values.imageDataLocation = imageDataLocation;
		return this;
	}

	public MoBIESettings tableDataLocation( String tableDataLocation )
	{
		this.values.tableDataLocation = tableDataLocation;
		return this;
	}

	public MoBIESettings gitTablesBranch( String tableDataBranch )
	{
		this.values.tableDataBranch = tableDataBranch;
		return this;
	}

	public MoBIESettings publicationURL( String publicationURL )
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
		private ImageDataFormat imageDataFormat = ImageDataFormat.BdvN5S3;
		private String projectLocation;
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

		public ImageDataFormat getImageDataStorageModality() { return imageDataFormat; }

		public String getImageDataLocation()
		{
			return imageDataLocation != null ? imageDataLocation : projectLocation;
		}

		public String getTableDataLocation()
		{
			return tableDataLocation != null ? tableDataLocation : projectLocation;
		}

		public String getTableDataBranch()
		{
			return tableDataBranch != null ? tableDataBranch : projectBranch;
		}

		public String getImageDataBranch()
		{
			return projectBranch;
		}

		public String getPublicationURL()
		{
			return publicationURL;
		}

		public String getProjectLocation()
		{
			return projectLocation;
		}
	}
}
