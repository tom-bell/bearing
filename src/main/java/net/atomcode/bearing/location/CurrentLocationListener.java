package net.atomcode.bearing.location;

import android.location.Location;

/**
 * Listens for current location updates
 */
public interface CurrentLocationListener
{
	public void onCurrentLocationUpdated(Location location);
}
