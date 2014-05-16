package net.atomcode.bearing.location;

import android.content.Context;

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
		locationProvider.requestSingleLocationUpdate(request, listener);
		return this;
	}
}
