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

/**
 * Gets the users current location using the best available service
 */
public class CurrentLocationTask implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, LocationListener, com.google.android.gms.location.LocationListener
{
	private LocationClient client;
	private LocationManager locationManager;
	private CurrentLocationListener listener;

	private Accuracy desiredAccuracy;

	public CurrentLocationTask(Context context)
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

	public void setCurrentLocationListener(CurrentLocationListener listener)
	{
		this.listener = listener;
	}

	public void execute(Accuracy accuracy)
	{
		if (client != null)
		{
			this.desiredAccuracy = accuracy;
			client.connect();
		}
		else
		{
			getCurrentLocationLegacy(accuracy);
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
					listener.onCurrentLocationUpdated(lastKnowUserLocation);
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

	/**
	 * Get the location using the legacy apis
	 * @param accuracy The desired accuracy for the request
	 */
	private void getCurrentLocationLegacy(Accuracy accuracy)
	{
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
				listener.onCurrentLocationUpdated(lastKnownUserLocation);
			}
		}

		locationManager.requestSingleUpdate(provider, this, Looper.getMainLooper());
	}

	@Override public void onLocationChanged(Location location)
	{
		if (client != null)
		{
			client.disconnect();
		}

		if (listener != null)
		{
			listener.onCurrentLocationUpdated(location);
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
