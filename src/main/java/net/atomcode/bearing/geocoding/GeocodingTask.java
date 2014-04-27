package net.atomcode.bearing.geocoding;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;

import java.util.List;
import java.util.Locale;

/**
 * Base Geocoding task, supplies listener and other definitions
 */
public abstract class GeocodingTask<T> extends AsyncTask<T, Void, List<Address>>
{
	private static final int DEFAULT_RESULT_COUNT = 10;

	protected static final String WEB_API_URL = "https://maps.googleapis.com/maps/api/geocode/json";

	public interface Listener
	{
		public void onSuccess(List<Address> locations);
		public void onFailure();
	}

	protected Context context;
	protected Locale locale;

	protected Listener listener;

	private T[] params;

	protected int resultCount;

	GeocodingTask(Context context, T[] params)
	{
		this(context, params, context.getResources().getConfiguration().locale);
	}

	GeocodingTask(Context context, T[] params, Locale locale)
	{
		this.context = context;
		this.locale = locale;

		this.params = params;

		// Set a default result count
		this.resultCount = DEFAULT_RESULT_COUNT;
	}

	/**
	 * Attach the given listener to the Geocoding task
	 */
	@SuppressWarnings("unused")
	public GeocodingTask listen(Listener listener)
	{
		this.listener = listener;
		return this;
	}

	/**
	 * Set the desired number of results from this query
	 */
	@SuppressWarnings("unused")
	public GeocodingTask results(int resultCount)
	{
		this.resultCount = resultCount;
		return this;
	}

	/**
	 * Begin the task execution. Returns the task for future cancellation if required
	 */
	@SuppressWarnings("unused, unchecked")
	public GeocodingTask start()
	{
		execute(params);
		return this;
	}

	/**
	 * Simple listener callbacks to check for valid return values
	 */
	@Override protected void onPostExecute(List<Address> address)
	{
		super.onPostExecute(address);
		if (address != null)
		{
			if (listener != null)
			{
				listener.onSuccess(address);
			}
		}
		else
		{
			listener.onFailure();
		}
	}

	/**
	 * Check to see if the device has native geocoding capability.
	 * @return {@code true} if ability present, {@code false} otherwise.
	 */
	protected boolean deviceHasNativeGeocoding()
	{
		return Geocoder.isPresent();
	}
}
