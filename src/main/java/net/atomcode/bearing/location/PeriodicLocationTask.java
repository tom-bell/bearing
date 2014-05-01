package net.atomcode.bearing.location;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;

import net.atomcode.bearing.Bearing;

/**
 * Gets the users current location using the best available service
 */
public class PeriodicLocationTask
{
	private static final long DEFAULT_INTERNAL_UPDATE_INTERVAL = 5 * 60 * 1000;

	public interface Listener
	{
		public void onStartedUpdating();
		public void onUpdate(Location location);
		public void onStoppedUpdating();
		public void onFailure();
	}

	private static class CurrentLocationListener implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, LocationListener, com.google.android.gms.location.LocationListener
	{
		private LocationClient client;
		private LocationManager locationManager;

		private Accuracy desiredAccuracy = Accuracy.MEDIUM;
		private float smallestDisplacement = -1.0f;
		private long fallbackTimeout = -1;

		private Listener listener;

		private Location lastReportedLocation;
		private long lastReportedTimestamp = -1;

		private long locationCheckRate = DEFAULT_INTERNAL_UPDATE_INTERVAL;

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
			LocationRequest request = new LocationRequest();
			request.setInterval(locationCheckRate);

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

			if (listener != null)
			{
				listener.onStartedUpdating();
			}

			client.requestLocationUpdates(request, this);
		}

		@Override public void onDisconnected()
		{
			if (listener != null)
			{
				listener.onStoppedUpdating();
			}
		}

		@Override public void onConnectionFailed(ConnectionResult connectionResult)
		{
			if (listener != null)
			{
				listener.onFailure();
			}
		}

		@Override public void onLocationChanged(Location currentLocation)
		{
			long currentTimestamp = System.currentTimeMillis() / 1000;
			long timeSinceLastReport = currentTimestamp - lastReportedTimestamp;

			if (lastReportedTimestamp == -1 || timeSinceLastReport > fallbackTimeout)
			{
				lastReportedLocation = currentLocation;
				lastReportedTimestamp = currentTimestamp;

				// Force report
				if (listener != null)
				{
					listener.onUpdate(currentLocation);
				}
				return;
			}

			if (smallestDisplacement != -1 && currentLocation.distanceTo(lastReportedLocation) > smallestDisplacement)
			{
				lastReportedLocation = currentLocation;
				lastReportedTimestamp = currentTimestamp;

				if (listener != null)
				{
					listener.onUpdate(currentLocation);
				}
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

	public PeriodicLocationTask(Context context)
	{
		locationServicesAvailable = Bearing.isLocationServicesAvailable(context);

		currentLocationListener = new CurrentLocationListener(context);
	}

	/**
	 * Set the listener for the location update.
	 */
	@SuppressWarnings("unused")
	public PeriodicLocationTask listen(Listener listener)
	{
		currentLocationListener.listener = listener;
		return this;
	}

	/**
	 * Set the desired accuracy for the request. A higher accuracy takes longer, but better
	 * pinpoints the users location.
	 */
	@SuppressWarnings("unused")
	public PeriodicLocationTask accuracy(Accuracy accuracy)
	{
		currentLocationListener.desiredAccuracy = accuracy;
		return this;
	}

	/**
	 * Set the desired distance between location updates. Updates will only occur if the user
	 * has moved more than the given distance.
	 */
	@SuppressWarnings("unused")
	public PeriodicLocationTask displacement(float displacementInMetres)
	{
		currentLocationListener.smallestDisplacement = displacementInMetres;
		return this;
	}

	/**
	 * Set the fallback timeout for location update, for if a user has not moved for an extended
	 * period of time.
	 */
	@SuppressWarnings("unused")
	public PeriodicLocationTask timeout(long timeout)
	{
		currentLocationListener.fallbackTimeout = timeout;
		return this;
	}

	/**
	 * Time between checks for current location. Higher rates use far more battery.
	 * A value >= 5 minutes is suggested. Default is 5 minutes.
	 */
	@SuppressWarnings("unused")
	public PeriodicLocationTask rate(long rate)
	{
		currentLocationListener.locationCheckRate = rate;
		return this;
	}

	/**
	 * Begin the lookup task using the set configuration.
	 * Returns the task for cancellation if required.
	 * ({@code PeriodicLocationTask.cancel()})
	 */
	@SuppressWarnings("unused")
	public PeriodicLocationTask start()
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
	 * Cancel the current task, preventing any further updates
	 */
	@SuppressWarnings("unused")
	public void cancel()
	{
		currentLocationListener.client.removeLocationUpdates(currentLocationListener);
		currentLocationListener.client.disconnect();
		currentLocationListener.locationManager.removeUpdates(currentLocationListener);

		if (currentLocationListener != null)
		{
			currentLocationListener.listener.onStoppedUpdating();
		}
	}

	/**
	 * Get the location using the legacy APIs
	 * @param accuracy The desired accuracy for the request
	 */
	private void getCurrentLocationLegacy(Accuracy accuracy)
	{
		LocationManager locationManager = currentLocationListener.locationManager;
		Listener listener = currentLocationListener.listener;

		int powerCriteria = Criteria.POWER_LOW;
		int accuracyCriteria = Criteria.ACCURACY_MEDIUM;

		switch (accuracy)
		{
			case LOW:
				powerCriteria = Criteria.POWER_LOW;
				accuracyCriteria = Criteria.ACCURACY_COARSE;
				break;
			case MEDIUM:
				powerCriteria = Criteria.POWER_MEDIUM;
				accuracyCriteria = Criteria.ACCURACY_MEDIUM;
				break;
			case HIGH:
				powerCriteria = Criteria.POWER_HIGH;
				accuracyCriteria = Criteria.ACCURACY_FINE;
		}

		Criteria criteria = new Criteria();
		criteria.setPowerRequirement(powerCriteria);
		criteria.setAccuracy(accuracyCriteria);

		String bestProvider = locationManager.getBestProvider(criteria, false);

		if (listener != null)
		{
			listener.onStartedUpdating();
		}

		// Every 5 minutes
		locationManager.requestLocationUpdates(bestProvider, currentLocationListener.locationCheckRate, 0, currentLocationListener);
	}
}
