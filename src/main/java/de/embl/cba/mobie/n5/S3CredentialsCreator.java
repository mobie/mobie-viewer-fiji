package de.embl.cba.mobie.n5;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;

public class S3CredentialsCreator
{
	public static AWSCredentialsProvider getCredentialsProvider( S3Authentication authentication )
	{
		switch ( authentication )
		{
			case Anonymous:
				return new AWSStaticCredentialsProvider( new AnonymousAWSCredentials() );
			case Protected:
				final DefaultAWSCredentialsProviderChain credentialsProvider = new DefaultAWSCredentialsProviderChain();
				checkCredentialsExistence( credentialsProvider );
				return credentialsProvider;
			default:
				throw new UnsupportedOperationException( "Authentication not supported: " + authentication );
		}
	}

	public static void checkCredentialsExistence( AWSCredentialsProvider credentialsProvider )
	{
		try
		{
			credentialsProvider.getCredentials();
		}
		catch ( Exception e )
		{
			throw  new RuntimeException( e ); // No credentials could be found
		}
	}
}
