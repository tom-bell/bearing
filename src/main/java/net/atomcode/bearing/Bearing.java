package net.atomcode.bearing;

import android.content.Context;
import android.location.LocationManager;

import net.atomcode.bearing.geocoding.GeocodingTask;
import net.atomcode.bearing.geocoding.GeocodingTaskListener;
import net.atomcode.bearing.geocoding.ReverseGeocodingTask;
import net.atomcode.bearing.location.Accuracy;
import net.atomcode.bearing.location.CurrentLocationListener;
import net.atomcode.bearing.location.CurrentLocationTask;

/**
 * Entry class for Bearing library. Has functions for all major actions
 */
public class Bearing
{
	/**
	 * Geocode query into latitude and longitude into a single, best-guess Address.
	 * @param context The context of the request
	 * @param query The string queried
	 * @param listener The listener to call back to
	 */
	public static GeocodingTask getAddressForQuery(Context context, String query, GeocodingTaskListener listener)
	{
		return getAddressListForQuery(context, query, listener, 1);
	}

	/**
	 * Geocode query into latitude and longitude.
	 *
	 * This request returns a list of 10 results by default. For configurable values see
	 * {@link net.atomcode.bearing.Bearing#getAddressListForQuery(android.content.Context, String, net.atomcode.bearing.geocoding.GeocodingTaskListener, int)}
	 *
	 * @param context The context of the request
	 * @param query The string queried
	 * @param listener The listener to call back to
	 */
	public static GeocodingTask getAddressListForQuery(Context context, String query, GeocodingTaskListener listener)
	{
		return getAddressListForQuery(context, query, listener, 10);
	}

	/**
	 * Geocode query into latitude and longitude
	 * @param context The context of the request
	 * @param query The string queried
	 * @param listener The listener to call back to
	 */
	public static GeocodingTask getAddressListForQuery(Context context, String query, GeocodingTaskListener listener, int resultCount)
	{
		GeocodingTask geocodingTask = new GeocodingTask(context);
		geocodingTask.setGeocodingTaskListener(listener);
		geocodingTask.setResultCount(resultCount);
		geocodingTask.execute(query);

		return geocodingTask;
	}

	/**
	 * Reverse geocode the location into an address. This method returns a single, best-guess location for the query.
	 * @param context The context of the request
	 * @param latitude The latitude to lookup
	 * @param longitude The longitude to check
	 * @param listener The listener to call back to
	 */
	public static ReverseGeocodingTask getAddressForLocation(Context context, Double latitude, Double longitude, GeocodingTaskListener listener)
	{
		return getAddressListForLocation(context, latitude, longitude, listener, 1);
	}

	/**
	 * Reverse geocode the location into an address. This method returns a list of default length 10.
	 * @param context The context of the request
	 * @param latitude The latitude to lookup
	 * @param longitude The longitude to check
	 * @param listener The listener to call back to
	 */
	public static ReverseGeocodingTask getAddressListForLocation(Context context, Double latitude, Double longitude, GeocodingTaskListener listener)
	{
		return getAddressListForLocation(context, latitude, longitude, listener,10);
	}

	/**
	 * Reverse geocode the location into an address
	 * @param context The context of the request
	 * @param latitude The latitude to lookup
	 * @param longitude The longitude to check
	 * @param listener The listener to call back to
	 */
	public static ReverseGeocodingTask getAddressListForLocation(Context context, Double latitude, Double longitude, GeocodingTaskListener listener, int resultCount)
	{
		ReverseGeocodingTask geocodingTask = new ReverseGeocodingTask(context);
		geocodingTask.setGeocodingTaskListener(listener);
		geocodingTask.setResultCount(resultCount);
		geocodingTask.execute(latitude, longitude);

		return geocodingTask;
	}

	/**
	 * Get the current location of the user to the given accuracy
	 * @param context The context of the request
	 * @param accuracy The accuracy to which the users location should match.
	 * @param listener The listener to call back to
	 */
	public static CurrentLocationTask getCurrentLocation(Context context, Accuracy accuracy, CurrentLocationListener listener)
	{
		CurrentLocationTask currentLocationTask = new CurrentLocationTask(context);
		currentLocationTask.setCurrentLocationListener(listener);
		currentLocationTask.execute(accuracy);

		return currentLocationTask;
	}

	/**
	 * Get the current location of the user to the closest 50 metres.
	 * @param context The context of the request
	 * @param listener The listener to call back to
	 */
	public static CurrentLocationTask getCurrentLocation(Context context, CurrentLocationListener listener)
	{
		return getCurrentLocation(context, Accuracy.MEDIUM, listener);
	}

	public static boolean isLocationServicesAvailable(Context context)
	{
		if (context == null)
		{
			throw new IllegalArgumentException("Context cannot be null on Bearing.isLocationServicesAvailable call");
		}

		LocationManager lm = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);

		boolean gps_enabled;
		boolean network_enabled;

		try
		{
			gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
			if (gps_enabled) return true;
		}
		catch(Exception ex)
		{
			// Ignore
		}

		try
		{
			network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
			if (network_enabled) return true;
		}
		catch(Exception ex)
		{
			// Ignore
		}

		return false;
	}
}
