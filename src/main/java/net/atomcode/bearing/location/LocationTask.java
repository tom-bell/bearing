package net.atomcode.bearing.location;

import android.content.Context;
import android.location.Location;

import net.atomcode.bearing.Bearing;
import net.atomcode.bearing.BearingTask;
import net.atomcode.bearing.location.provider.GMSLocationProvider;
import net.atomcode.bearing.location.provider.LegacyLocationProvider;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Base location task for acquiring locations
 */
public abstract class LocationTask implements BearingTask
{
	/**
	 * Do nothing when a timeout occurs
	 */
	public static final int FALLBACK_NONE = 0x0;

	/**
	 * Use a cached location when a timeout occurs
	 */
	public static final int FALLBACK_CACHE = 0x1;

	protected boolean isUsingLegacyServices;

	protected LocationProvider locationProvider;
	protected LocationProviderRequest request;

	protected LocationListener listener;

	protected long timeout = 3000; // 3 seconds by default
	protected boolean running = false;

	protected int fallback = FALLBACK_NONE; // No fallback by default

	protected String taskId;

	public LocationTask(Context context)
	{
		isUsingLegacyServices = !Bearing.isLocationServicesAvailable(context);
		if (isUsingLegacyServices)
		{
			locationProvider = LegacyLocationProvider.getInstance();
		}
		else
		{
			locationProvider = GMSLocationProvider.getInstance();
		}
		locationProvider.create(context);

		request = new LocationProviderRequest();
	}

	@Override
	public BearingTask start()
	{
		running = true;
		new Timer().schedule(new TimerTask() {
			@Override
			public void run()
			{
				if (isRunning())
				{
					cancel();
					if (listener != null)
					{
						listener.onTimeout();
						handleTimeoutFallback();
					}
				}
			}
		}, timeout);

		return this;
	}

	@Override
	public void cancel()
	{
		if (taskId != null)
		{
			locationProvider.cancelUpdates(taskId);
		}
		running = false;
	}

	/**
	 * Listen for location updates
	 */
	public LocationTask listen(LocationListener listener)
	{
		this.listener = listener;
		return this;
	}

	@Override
	public boolean isRunning()
	{
		return running;
	}

	/*
	 * ==============================================
	 * LOCATION TASK API
	 * ==============================================
	 */

	/**
	 * Set the accuracy of the location request(s)
	 * @param accuracy The accuracy to which the location should be gathered
	 */
	public LocationTask accuracy(Accuracy accuracy)
	{
		request.accuracy = accuracy;
		return this;
	}

	/**
	 * Whether to use a cached location if available,
	 * and how old does the location need to be to be treated as valid
	 * @param use Whether to use the cached location
	 * @param expiry How old does a cached location have to be to be invalid?
	 */
	public LocationTask cache(boolean use, long expiry)
	{
		request.useCache = use;
		request.cacheExpiry = expiry;
		return this;
	}

	/**
	 * Timeout the request if it has not completed in the given time
	 * @param timeout The timeout in milliseconds
	 */
	public LocationTask timeout(long timeout)
	{
		this.timeout = timeout;
		return this;
	}

	/**
	 * Fallback for if the timeout is reached
	 */
	public LocationTask fallback(int fallback)
	{
		this.fallback = fallback;
		return this;
	}

	/*
	 * ==============================================
	 * INTERNAL METHODS
	 * ==============================================
	 */

	/**
	 * Handle the timeout fallback here.
	 * listener is non-null at this point.
	 */
	private void handleTimeoutFallback()
	{
		if (fallback == FALLBACK_CACHE)
		{
			Location cachedLocation = locationProvider.getLastKnownLocation(request);
			if (cachedLocation != null)
			{
				listener.onUpdate(cachedLocation);
			}
			else
			{
				listener.onFailure();
			}
		}
	}
}
