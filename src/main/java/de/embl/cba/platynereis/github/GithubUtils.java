package de.embl.cba.platynereis.github;

public abstract class GithubUtils
{
	public static RepoUrlAndPath rawUrlToRepoUrlAndPath( String datasetLocation )
	{
		final RepoUrlAndPath repoUrlAndPath = new RepoUrlAndPath();
		final String[] split = datasetLocation.split( "/" );
		final String user = split[ 3 ];
		final String repo = split[ 4 ];
		repoUrlAndPath.repoUrl = "https://github.com/" + user + "/" + repo;
		repoUrlAndPath.path = "";
		for ( int i = 6; i < split.length; i++ )
		{
			repoUrlAndPath.path += split[ i ] + "/";
		}
		return repoUrlAndPath;
	}
}
