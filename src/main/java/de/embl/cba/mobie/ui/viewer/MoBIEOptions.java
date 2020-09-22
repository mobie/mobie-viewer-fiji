package de.embl.cba.mobie.ui.viewer;

public class MoBIEOptions
{
	public final Values values = new Values();

	public enum ImageDataLocationType
	{
		Local,
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

	public MoBIEOptions gitBranch( String gitBranch )
	{
		this.values.projectBranch = gitBranch;
		return this;
	}

	public MoBIEOptions imageDataLocationType( ImageDataLocationType imageDataLocationType )
	{
		this.values.imageDataLocationType = imageDataLocationType;
		return this;
	}

	public MoBIEOptions imageDataRootPath( String imageDataRootPath )
	{
		this.values.imageDataRootPath = imageDataRootPath;
		return this;
	}

	public MoBIEOptions tableDataLocation( String tableDataLocation )
	{
		this.values.tableDataLocation = tableDataLocation;
		return this;
	}

	public MoBIEOptions tableDataBranch( String tableDataBranch )
	{
		this.values.tableDataBranch = tableDataBranch;
		return this;
	}

	public static class Values
	{
		private String dataset;
		private String projectBranch = "master"; // project and images
		private String tableDataBranch;
		private ImageDataLocationType imageDataLocationType = ImageDataLocationType.S3;
		private String imageDataRootPath;
		private String tableDataLocation;

		public String getDataset()
		{
			return dataset;
		}
		public String getProjectBranch()
		{
			return projectBranch;
		}
		public ImageDataLocationType getImageDataLocationType() { return imageDataLocationType; }

		public String getImageDataRootPath()
		{
			return imageDataRootPath;
		}

		public String getTableDataLocation()
		{
			return tableDataLocation;
		}

		public String getTableDataBranch()
		{
			return tableDataBranch != null ? tableDataBranch : projectBranch;
		}
	}
}
