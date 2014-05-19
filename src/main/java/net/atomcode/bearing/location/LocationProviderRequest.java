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

	/*
	 * Tracking
	 */
	public float trackingDisplacement = -1.0f; // No displacement when tracking
	public long trackingRate = 20 * 60 * 1000; // 20 min tracking default
	public long trackingFallback = 30 * 60 * 1000; // 30 mins default fallback

}
