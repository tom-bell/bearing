package net.atomcode.bearing.location;

import android.content.Context;
import android.location.Location;

/**
 * Gets the users current location using the best available service
 */
public class CurrentLocationTask extends LocationTask
{
	public CurrentLocationTask(Context context)
	{
		super(context);
	}

	/**
	 * Begin the lookup task using the set configuration.
	 * Returns the task for cancellation if required.
	 */
	@SuppressWarnings("unused")
	public CurrentLocationTask start()
	{
		super.start();
		this.taskId = locationProvider.requestSingleLocationUpdate(request, new LocationListener()
		{
			@Override public void onUpdate(Location location)
			{
				if (running)
				{
					// Cancel current task
					running = false;
                    notifyLocationUpdate(location);
				}
			}

			@Override public void onFailure()
			{
				notifyFailure();
			}

			@Override public void onTimeout()
			{
				notifyTimeout();
			}
		});
		return this;
	}
}
