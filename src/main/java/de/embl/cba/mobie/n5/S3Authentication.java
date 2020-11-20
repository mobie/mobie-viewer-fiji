package de.embl.cba.mobie.n5;

/**
 * It seems that the way S3 works is that when a user has no credentials it means anonymous,
 * but as soon as you provide some credentials it tries to get access with those,
 * which indeed don't have access for that specific bucket.
 * So it seems the way to go is to define in the application whether
 * you want to use anonymous access or credentials based access
 */
public enum S3Authentication
{
	Anonymous,
	Protected
}
