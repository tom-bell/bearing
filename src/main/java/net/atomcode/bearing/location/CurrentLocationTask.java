package net.atomcode.bearing.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;

import net.atomcode.bearing.Bearing;

/**
 * Gets the users current location using the best available service
 */
public class CurrentLocationTask
{
	public interface Listener
	{
		public void onSuccess(Location location);
		public void onFailure();
	}

	private static class CurrentLocationListener implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, LocationListener, com.google.android.gms.location.LocationListener
	{
		private LocationClient client;
		private LocationManager locationManager;

		private Accuracy desiredAccuracy = Accuracy.MEDIUM;

		private Listener listener;

		CurrentLocationListener(Context context)
		{
			if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS)
			{
				client = new LocationClient(context, this, this);
			}
			else
			{
				locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
			}
		}

		@Override public void onConnected(Bundle bundle)
		{
			Location lastKnowUserLocation = client.getLastLocation();
			if (lastKnowUserLocation != null)
			{
				// Check to see if it's good enough for the request
				if (lastKnowUserLocation.getAccuracy() < desiredAccuracy.value)
				{
					if (listener != null)
					{
						listener.onSuccess(lastKnowUserLocation);
					}
				}
			}
			else
			{
				LocationRequest request = new LocationRequest();
				request.setNumUpdates(1); // Single update
				int priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
				switch (desiredAccuracy)
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
				request.setPriority(priority);

				client.requestLocationUpdates(request, this);
			}
		}

		@Override public void onDisconnected()
		{

		}

		@Override public void onConnectionFailed(ConnectionResult connectionResult)
		{
		}

		@Override public void onLocationChanged(Location location)
		{
			if (client != null)
			{
				client.disconnect();
			}

			if (listener != null)
			{
				listener.onSuccess(location);
			}
		}

		@Override public void onStatusChanged(String provider, int status, Bundle extras)
		{
			// Ignore TODO: Handle this
		}

		@Override public void onProviderEnabled(String provider)
		{
			// Ignore TODO: Handle this
		}

		@Override public void onProviderDisabled(String provider)
		{
			// Ignore TODO: Handle this
		}
	}

	private CurrentLocationListener currentLocationListener;
	private boolean locationServicesAvailable;

	public CurrentLocationTask(Context context)
	{
		locationServicesAvailable = Bearing.isLocationServicesAvailable(context);

		currentLocationListener = new CurrentLocationListener(context);
	}

	/**
	 * Set the listener for the location update.
	 */
	@SuppressWarnings("unused") // public API
	public CurrentLocationTask listen(Listener listener)
	{
		currentLocationListener.listener = listener;
		return this;
	}

	/**
	 * Set the desired accuracy for the request. A higher accuracy takes longer, but better
	 * pinpoints the users location.
	 */
	@SuppressWarnings("unused")
	public CurrentLocationTask accuracy(Accuracy accuracy)
	{
		currentLocationListener.desiredAccuracy = accuracy;
		return this;
	}

	/**
	 * Begin the lookup task using the set configuration.
	 * Returns the task for cancellation if required.
	 * ({@code CurrentLocationTask.cancel()} Currently not available for Current Location requests)
	 */
	@SuppressWarnings("unused")
	public CurrentLocationTask start()
	{
		if (!locationServicesAvailable)
		{
			if (currentLocationListener.listener != null)
			{
				currentLocationListener.listener.onFailure();
			}
			return this;
		}

		if (currentLocationListener.client != null)
		{
			currentLocationListener.client.connect();
		}
		else
		{
			getCurrentLocationLegacy(currentLocationListener.desiredAccuracy);
		}

		return this;
	}

	/**
	 * Get the location using the legacy APIs
	 * @param accuracy The desired accuracy for the request
	 */
	private void getCurrentLocationLegacy(Accuracy accuracy)
	{
		LocationManager locationManager = currentLocationListener.locationManager;
		Listener listener = currentLocationListener.listener;

		String provider = null;
		switch (accuracy)
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

		Location lastKnownUserLocation = locationManager.getLastKnownLocation(provider);
		if (lastKnownUserLocation != null && lastKnownUserLocation.getAccuracy() < accuracy.value)
		{
			if (listener != null)
			{
				listener.onSuccess(lastKnownUserLocation);
			}
		}

		locationManager.requestSingleUpdate(provider, currentLocationListener, Looper.getMainLooper());
	}
}
