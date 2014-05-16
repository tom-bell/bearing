package net.atomcode.bearing.location.provider;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

import net.atomcode.bearing.location.LocationListener;
import net.atomcode.bearing.location.LocationProvider;
import net.atomcode.bearing.location.LocationProviderRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Simple location provider using the legacy android location services
 */
public class LegacyLocationProvider implements LocationProvider
{
	private static LegacyLocationProvider instance;

	public static LegacyLocationProvider getInstance()
	{
		if (instance == null)
		{
			instance = new LegacyLocationProvider();
		}
		return instance;
	}

	private LocationManager locationManager;

	private Map<String, android.location.LocationListener> runningRequests;

	@Override public void create(Context context)
	{
		locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);

		runningRequests = new HashMap<String, android.location.LocationListener>();
	}

	@Override public void destroy()
	{
		for (android.location.LocationListener runningRequest : runningRequests.values())
		{
			locationManager.removeUpdates(runningRequest);
		}
		runningRequests.clear();
	}

	@Override
	public Location getLastKnownLocation(LocationProviderRequest request)
	{
		String provider = getProviderForRequest(request);
		return locationManager.getLastKnownLocation(provider);
	}

	@Override
	public String requestSingleLocationUpdate(LocationProviderRequest request, final LocationListener listener)
	{
		String provider = getProviderForRequest(request);

		if (request.useCache)
		{
			Location lastKnownUserLocation = locationManager.getLastKnownLocation(provider);

			// Check if last known location is valid
			if (lastKnownUserLocation != null &&
					System.currentTimeMillis() - lastKnownUserLocation.getTime() < request.cacheExpiry)
			{
				if (lastKnownUserLocation.getAccuracy() < request.accuracy.value)
				{
					if (listener != null)
					{
						listener.onSuccess(lastKnownUserLocation);
						return null;
					}
				}
			}
		}

		String requestId = UUID.randomUUID().toString();

		runningRequests.put(requestId, new android.location.LocationListener()
		{
			@Override public void onLocationChanged(Location location)
			{
				if (listener != null)
				{
					listener.onSuccess(location);
				}
				runningRequests.remove(this);
			}

			@Override public void onStatusChanged(String provider, int status, Bundle extras)
			{

			}

			@Override public void onProviderEnabled(String provider)
			{

			}

			@Override public void onProviderDisabled(String provider)
			{

			}
		});

		locationManager.requestSingleUpdate(provider, runningRequests.get(requestId), Looper.getMainLooper());

		return requestId;
	}

	@Override
	public String requestRecurringLocationUpdates(LocationProviderRequest request, LocationListener listener)
	{
		return null;
	}

	@Override
	public void cancelUpdates(String requestId)
	{
		if (runningRequests.containsKey(requestId))
		{
			locationManager.removeUpdates(runningRequests.get(requestId));
			runningRequests.remove(requestId);
		}
	}

	/**
	 * Get the provider for the given request
	 */
	private String getProviderForRequest(LocationProviderRequest request)
	{
		String provider = null;
		switch (request.accuracy)
		{
			case LOW:
				provider = LocationManager.PASSIVE_PROVIDER;
				if (locationManager.isProviderEnabled(provider))
				{
					break;
				}
			case MEDIUM:
				provider = LocationManager.NETWORK_PROVIDER;
				if (locationManager.isProviderEnabled(provider))
				{
					break;
				}
			case HIGH:
				provider = LocationManager.GPS_PROVIDER;
		}
		return provider;
	}

}
