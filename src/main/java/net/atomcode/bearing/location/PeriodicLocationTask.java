package net.atomcode.bearing.location;

import android.content.Context;
import android.location.Location;

/**
 * Gets the users current location over distance using the best available service
 */
public class PeriodicLocationTask extends LocationTask
{
	public PeriodicLocationTask(Context context)
	{
		super(context);
	}

	/**
	 * Begin the lookup task using the set configuration.
	 * Returns the task for cancellation if required.
	 */
	@SuppressWarnings("unused")
	public PeriodicLocationTask start()
	{
		super.start();
		this.taskId = locationProvider.requestRecurringLocationUpdates(request, new LocationListener() {
			@Override
			public void onUpdate(Location location)
			{
				notifyLocationUpdate(location);
			}

			@Override
			public void onFailure()
			{
				notifyFailure();
			}

			@Override
			public void onTimeout()
			{
				notifyTimeout();
			}
		});
		return this;
	}

	/**
	 * Set the desired distance between location updates. Updates will only occur if the user
	 * has moved more than the given distance.
	 */
	@SuppressWarnings("unused")
	public PeriodicLocationTask displacement(float displacementInMetres)
	{
		request.trackingDisplacement = displacementInMetres;
		return this;
	}

	/**
	 * Set the fallback timeout for location update, for if a user has not moved for an extended
	 * period of time.
	 */
	@SuppressWarnings("unused")
	public PeriodicLocationTask timeout(long timeout)
	{
		request.trackingFallback = timeout;
		return this;
	}

	/**
	 * Time between checks for current location in milliseconds. Higher rates use far more battery.
	 * A value >= 5 minutes is suggested. Default is 20 minutes.
	 */
	@SuppressWarnings("unused")
	public PeriodicLocationTask rate(long rate)
	{
		request.trackingRate = rate;
		return this;
	}

}
