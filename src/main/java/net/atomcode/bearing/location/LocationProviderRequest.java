package net.atomcode.bearing.location;

/**
 * Wrapper for a request to a location provider
 */
public class LocationProviderRequest
{
	/*
	 * Location accuracy
	 */
	public Accuracy accuracy = Accuracy.MEDIUM; // Medium accuracy by default

	/*
	 * Cache
	 */
	public boolean useCache= true; // Use cache by default
	public long cacheExpiry = 60 * 60 * 1000; // Expiry of 1 hour by default
}
