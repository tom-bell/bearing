package net.atomcode.bearing.location.provider;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;

import net.atomcode.bearing.location.LocationListener;
import net.atomcode.bearing.location.LocationProvider;
import net.atomcode.bearing.location.LocationProviderRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Provide location using Google Play services
 */
public class GMSLocationProvider implements LocationProvider, GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener
{
	private static GMSLocationProvider instance;

	public static GMSLocationProvider getInstance()
	{
		if (instance == null)
		{
			instance = new GMSLocationProvider();
		}
		return instance;
	}

	private LocationClient locationClient;

	private HashMap<String, Runnable> pendingRequests;
	private Map<String, com.google.android.gms.location.LocationListener> runningRequests;

	@Override
	public void create(Context context)
	{
		pendingRequests = new HashMap<String, Runnable>();
		runningRequests = new HashMap<String, com.google.android.gms.location.LocationListener>();
		locationClient = new LocationClient(context, this, this);
	}

	@Override
	public void destroy()
	{
		pendingRequests.clear();

		if (locationClient.isConnected() || locationClient.isConnecting())
		{
			for (com.google.android.gms.location.LocationListener runningRequest : runningRequests.values())
			{
				locationClient.removeLocationUpdates(runningRequest);
			}
			runningRequests.clear();

			locationClient.disconnect();
		}
	}

	@Override
	public Location getLastKnownLocation(LocationProviderRequest request)
	{
		if (locationClient.isConnected())
		{
			return locationClient.getLastLocation();
		}
		return null;
	}

	@Override
	public String requestSingleLocationUpdate(final LocationProviderRequest request, final LocationListener listener)
	{
		final String requestId = UUID.randomUUID().toString();

		if (!locationClient.isConnected())
		{
			pendingRequests.put(requestId, new Runnable()
			{
				@Override public void run()
				{
					internalRequestSingleUpdate(requestId, request, listener);
				}
			});
			locationClient.connect();
		}
		else
		{
			internalRequestSingleUpdate(requestId, request, listener);
		}
		return requestId;
	}

	@Override
	public String requestRecurringLocationUpdates(final LocationProviderRequest request, final LocationListener listener)
	{
		final String requestId = UUID.randomUUID().toString();

		if (!locationClient.isConnected())
		{
			pendingRequests.put(requestId, new Runnable() {
				@Override public void run()
				{
					internalRequestRecurringUpdates(requestId, request, listener);
				}
			});
			locationClient.connect();
		}
		else
		{
			internalRequestRecurringUpdates(requestId, request, listener);
		}
		return requestId;
	}

	@Override
	public void cancelUpdates(String requestId)
	{
		if (pendingRequests.containsKey(requestId))
		{
			pendingRequests.remove(requestId);
		}

		if (runningRequests.containsKey(requestId))
		{
			locationClient.removeLocationUpdates(runningRequests.get(requestId));
			runningRequests.remove(requestId);

			if (runningRequests.size() == 0)
			{
				locationClient.disconnect();
			}
		}
	}

	/**
	 * Internal request for recurring updates
	 */
	private void internalRequestRecurringUpdates(final String requestId, final LocationProviderRequest request, final LocationListener listener)
	{
		LocationRequest gmsRequest = getRecurringLocationRequestForBearingRequest(request);

		runningRequests.put(requestId, new com.google.android.gms.location.LocationListener()
		{
			private long lastReportedTimestamp = -1;
			private Location lastReportedLocation;

			@Override public void onLocationChanged(Location location)
			{
				long currentTimestamp = System.currentTimeMillis() / 1000;
				long timeSinceLastReport = currentTimestamp - lastReportedTimestamp;

				Log.d("Bearing Location Tracker", "onLocationChanged last reported: " + timeSinceLastReport + " seconds ago (Fallback at " + request.trackingFallback / 1000 + ")");

				if (lastReportedTimestamp == -1 || timeSinceLastReport > (request.trackingFallback / 1000))
				{
					Log.d("Bearing Location Tracker", "Tracking fallback, forcing update");
					lastReportedLocation = location;
					lastReportedTimestamp = currentTimestamp;

					// Force report
					if (listener != null)
					{
						listener.onUpdate(location);
					}
					return;
				}

				if (request.trackingDisplacement != -1 && location.distanceTo(lastReportedLocation) > request.trackingDisplacement)
				{
					lastReportedLocation = location;
					lastReportedTimestamp = currentTimestamp;

					if (listener != null)
					{
						listener.onUpdate(location);
					}
				}
			}
		});

		locationClient.requestLocationUpdates(gmsRequest, runningRequests.get(requestId));
	}

	/**
	 * Convert bearing request to GMS location request
	 */
	private LocationRequest getRecurringLocationRequestForBearingRequest(LocationProviderRequest request)
	{
		LocationRequest gmsRequest = new LocationRequest();

		int priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
		switch (request.accuracy)
		{
			case LOW:
				priority = LocationRequest.PRIORITY_LOW_POWER;
				break;
			case MEDIUM:
				priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
				break;
			case HIGH:
				priority = LocationRequest.PRIORITY_HIGH_ACCURACY;
		}
		gmsRequest.setPriority(priority);

		gmsRequest.setFastestInterval(request.trackingRate);
		gmsRequest.setInterval(request.trackingRate);

		return gmsRequest;
	}

	/**
	 * Make request for a single location update
	 */
	private void internalRequestSingleUpdate(final String requestId, final LocationProviderRequest request, final LocationListener listener)
	{
		LocationRequest gmsRequest = getSingleLocationRequestForBearingRequest(request);

		runningRequests.put(requestId, new com.google.android.gms.location.LocationListener()
		{
			@Override public void onLocationChanged(Location location)
			{
				if (listener != null)
				{
					listener.onUpdate(location);
				}

				runningRequests.remove(requestId);

				if (runningRequests.size() == 0)
				{
					locationClient.disconnect();
				}
			}
		});

		locationClient.requestLocationUpdates(gmsRequest, runningRequests.get(requestId));
	}

	/**
	 * Convert bearing request to GMS Location request
	 */
	private LocationRequest getSingleLocationRequestForBearingRequest(LocationProviderRequest request)
	{
		LocationRequest gmsRequest = new LocationRequest();
		gmsRequest.setNumUpdates(1); // Single update
		int priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
		switch (request.accuracy)
		{
			case LOW:
				priority = LocationRequest.PRIORITY_LOW_POWER;
				break;
			case MEDIUM:
				priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
				break;
			case HIGH:
				priority = LocationRequest.PRIORITY_HIGH_ACCURACY;
		}
		gmsRequest.setPriority(priority);
		return gmsRequest;
	}

	/*
	 * ========================================================
	 * LocationListener and GooglePlayServicesClient callbacks
	 * ========================================================
	 */

	@Override public void onConnected(Bundle bundle)
	{
		ArrayList<String> executedRequests = new ArrayList<String>(pendingRequests.size());

		// Connected. Perform pending requests
		for (String key : pendingRequests.keySet())
		{
			Runnable action = pendingRequests.get(key);
			action.run();
			executedRequests.add(key);
		}

		for (String executedRequest : executedRequests)
		{
			pendingRequests.remove(executedRequest);
		}
	}

	@Override public void onDisconnected()
	{
		// Disconnected
	}

	@Override public void onConnectionFailed(ConnectionResult connectionResult)
	{

	}
}
