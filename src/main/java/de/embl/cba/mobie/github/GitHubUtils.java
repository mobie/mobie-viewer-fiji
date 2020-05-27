package de.embl.cba.mobie.github;

public abstract class GitHubUtils
{
	public static GitLocation rawUrlToGitLocation( String datasetLocation )
	{
		final GitLocation gitLocation = new GitLocation();
		final String[] split = datasetLocation.split( "/" );
		final String user = split[ 3 ];
		final String repo = split[ 4 ];
		gitLocation.branch = split[ 5 ];
		gitLocation.repoUrl = "https://github.com/" + user + "/" + repo;
		gitLocation.path = "";
		for ( int i = 6; i < split.length; i++ )
		{
			gitLocation.path += split[ i ] + "/";
		}
		return gitLocation;
	}
}
