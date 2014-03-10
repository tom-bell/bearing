package net.atomcode.bearing.geocoding;

import android.location.Address;

import java.util.List;

/**
 * Listener for Geocoding tasks. Returns when geocoding has completed.
 */
public interface GeocodingTaskListener
{
	public void onLocationGeocoded(List<Address> addressList);
	public void onLocationGeocodingFailed();
}
