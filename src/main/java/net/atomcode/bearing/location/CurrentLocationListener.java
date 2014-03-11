package net.atomcode.bearing.location;

import android.location.Location;

/**
 * Listens for current location updates
 */
public abstract class CurrentLocationListener
{
	public abstract void onCurrentLocationUpdated(Location location);

	/**
	 * Called when location services are determined to be unavailable.
	 */
	public void onLocationServicesUnavailable()
	{
		// Override for action
	}
}
