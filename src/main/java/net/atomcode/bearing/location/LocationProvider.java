package net.atomcode.bearing.location;

import android.content.Context;
import android.location.Location;

/**
 * Simple wrapper for location providers
 */
public interface LocationProvider
{
	/**
	 * Prepare the provider for use, connecting to remote services etc.
	 */
	public void create(Context context);

	/**
	 * Shutdown the provider, disconnecting from remote services.
	 */
	public void destroy();

	/**
	 * Get the last known cached location
	 */
	public Location getLastKnownLocation(LocationProviderRequest request);

	/**
	 * Get a single location update for this provider
	 * @param request The request containing the location update type
	 * @param listener The listener to use to get the callback
	 * @return A request identifier to cancel a request if required
	 */
	public String requestSingleLocationUpdate(LocationProviderRequest request, LocationListener listener);

	/**
	 * Get recurring location updates for this provider
	 * @param request The request containing the location update type
	 * @param listener The listener to use to get the callback
	 * @return A request identifier to cancel a request if required
	 */
	public String requestRecurringLocationUpdates(LocationProviderRequest request, LocationListener listener);

	/**
	 * Cancel a currently running request using the given request id
	 * @param requestId The id to use when cancelling a request
	 */
	public void cancelUpdates(String requestId);
}
