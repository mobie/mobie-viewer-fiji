package de.embl.cba.mobie.viewer;

public class ViewerOptions
{
	public final Values values = new Values();

	private String dataset;
	private String gitBranch;

	public static ViewerOptions options()
	{
		return new ViewerOptions();
	}

	public ViewerOptions dataset( String dataset )
	{
		this.values.dataset = dataset;
		return this;
	}


	public ViewerOptions gitBranch( String gitBranch )
	{
		this.values.gitBranch = gitBranch;
		return this;
	}

	public static class Values
	{
		private String dataset;
		private String gitBranch = "master";

		public String getDataset()
		{
			return dataset;
		}

		public String getGitBranch()
		{
			return gitBranch;
		}
	}
}
