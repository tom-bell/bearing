package net.atomcode.bearing.location;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;

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
	 * Use a cached location when a timeout occurs
	 */
	public static final int FALLBACK_CACHE = 0x1;

	protected boolean isUsingLegacyServices;

	protected LocationProvider locationProvider;
	protected LocationProviderRequest request;

	protected LocationListener listener;

	protected int fallback = FALLBACK_NONE; // No fallback by default
	protected long timeout = 0; // > 0 means no timeout
	protected boolean running = false;

	protected String taskId;

	private final Handler mainThreadHandler;

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

		mainThreadHandler = new Handler(Looper.getMainLooper());
	}

	@Override
	public BearingTask start()
	{
		running = true;
		if (timeout > 0)
		{
			new Timer().schedule(new TimerTask()
			{
				@Override
				public void run()
				{
					if (isRunning())
					{
						LocationTask.this.cancel();
						notifyTimeout();
						handleTimeoutFallback();
					}
				}
			}, timeout);
		}

		return this;
	}

	@Override
	public void cancel()
	{
		running = false;
		if (taskId != null)
		{
			locationProvider.cancelUpdates(taskId);
		}
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
	 * Fallback for if the timeout is reached
	 */
	@Override
	public LocationTask fallback(int fallback, long timeout)
	{
		this.fallback = fallback;
		this.timeout = timeout;
		return this;
	}

	/*
	 * ==============================================
	 * INTERNAL METHODS
	 * ==============================================
	 */

	private void notifyEvent(Runnable callback)
	{
		if (listener == null)
		{
			return;
		}
		mainThreadHandler.post(callback);
	}

	protected void notifyLocationUpdate(final Location location)
	{
		notifyEvent(new Runnable()
		{
			@Override
			public void run()
			{
				listener.onUpdate(location);
			}
		});
	}

	protected void notifyTimeout()
	{
		notifyEvent(new Runnable() {
			@Override
			public void run()
			{
				listener.onTimeout();
			}
		});
	}

	protected void notifyFailure()
	{
		notifyEvent(new Runnable() {
			@Override
			public void run()
			{
				listener.onFailure();
			}
		});
	}

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
				notifyLocationUpdate(cachedLocation);
			}
			else
			{
				notifyFailure();
			}
		}
	}
}
