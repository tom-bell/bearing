package net.atomcode.bearing.location.provider;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import net.atomcode.bearing.location.LocationListener;
import net.atomcode.bearing.location.LocationProvider;
import net.atomcode.bearing.location.LocationProviderRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Provide location using Google Play services
 */
public class GMSLocationProvider implements LocationProvider, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
	private static final boolean LOG = false;

	private static GMSLocationProvider instance;

	public static GMSLocationProvider getInstance()
	{
		if (instance == null)
		{
			instance = new GMSLocationProvider();
		}
		return instance;
	}

	private GoogleApiClient apiClient;

	private HashMap<String, Runnable> pendingRequests;
	private Map<String, com.google.android.gms.location.LocationListener> runningRequests;

	@Override
	public void create(Context context)
	{
		pendingRequests = new HashMap<>();
		runningRequests = new HashMap<>();
		apiClient = new GoogleApiClient.Builder(context)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(LocationServices.API)
				.build();
	}

	@Override
	public void destroy()
	{
		pendingRequests.clear();

		if (apiClient.isConnected() || apiClient.isConnecting())
		{
			for (com.google.android.gms.location.LocationListener runningRequest : runningRequests.values())
			{
				LocationServices.FusedLocationApi.removeLocationUpdates(apiClient, runningRequest);
			}
			runningRequests.clear();

			apiClient.disconnect();
		}
	}

	@Override
	public Location getLastKnownLocation(LocationProviderRequest request)
	{
		if (apiClient.isConnected())
		{
			return LocationServices.FusedLocationApi.getLastLocation(apiClient);
		}
		return null;
	}

	@Override
	public String requestSingleLocationUpdate(final LocationProviderRequest request, final LocationListener listener)
	{
		final String requestId = UUID.randomUUID().toString();

		if (!apiClient.isConnected())
		{
			pendingRequests.put(requestId, new Runnable()
			{
				@Override public void run()
				{
					internalRequestSingleUpdate(requestId, request, listener);
				}
			});
			apiClient.connect();
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

		if (!apiClient.isConnected())
		{
			pendingRequests.put(requestId, new Runnable() {
				@Override public void run()
				{
					internalRequestRecurringUpdates(requestId, request, listener);
				}
			});
			apiClient.connect();
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
			LocationServices.FusedLocationApi.removeLocationUpdates(apiClient, runningRequests.get(requestId));
			runningRequests.remove(requestId);

			if (runningRequests.size() == 0)
			{
				apiClient.disconnect();
			}
		}
	}

	/**
	 * Internal request for recurring updates
	 */
	private void internalRequestRecurringUpdates(final String requestId, final LocationProviderRequest request, final LocationListener listener)
	{
		final LocationRequest gmsRequest = getRecurringLocationRequestForBearingRequest(request);

		runningRequests.put(requestId, new com.google.android.gms.location.LocationListener()
		{
			private long lastReportedTimestamp = -1;
			private Location lastReportedLocation;

			@Override public void onLocationChanged(Location location)
			{
				long currentTimestamp = System.currentTimeMillis() / 1000;
				long timeSinceLastReport = currentTimestamp - lastReportedTimestamp;

				if (LOG)
				{
					Log.d("Bearing Location Tracker", "onLocationChanged last reported: " + timeSinceLastReport + " seconds ago (Fallback at " + request.trackingFallback / 1000 + ")");
				}

				if (lastReportedTimestamp == -1 || timeSinceLastReport > (request.trackingFallback / 1000))
				{
					if (LOG)
					{
						Log.d("Bearing Location Tracker", "Tracking fallback, forcing update");
					}
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

		if (apiClient.isConnected())
		{
			LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, gmsRequest, runningRequests.get(requestId));
		}
		else
		{
			final String connectRequestId = UUID.randomUUID().toString();
			pendingRequests.put(connectRequestId, new Runnable()
			{
				@Override public void run()
				{
					LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, gmsRequest, runningRequests.get(requestId));
				}
			});
		}
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
					apiClient.disconnect();
				}
			}
		});

		LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, gmsRequest, runningRequests.get(requestId));
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
		// Connected. Perform pending requests
		for (Runnable runnable : pendingRequests.values())
		{
			runnable.run();
		}

		pendingRequests.clear();
	}

	@Override
	public void onConnectionSuspended(int i)
	{

	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult)
	{

	}
}
