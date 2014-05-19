package net.atomcode.bearing.location;

import android.location.Location;

import net.atomcode.bearing.BearingListener;

/**
 * Listener for location updates
 */
public abstract class LocationListener implements BearingListener
{
	public abstract void onUpdate(Location location);

	// Do nothing here, allows for simpler listeners
	@Override public void onTimeout() {}
	@Override public void onFailure() {}
}
