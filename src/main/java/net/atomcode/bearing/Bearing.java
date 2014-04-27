package net.atomcode.bearing;

import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;

import com.google.android.gms.maps.model.LatLng;

import net.atomcode.bearing.geocoding.GeocodingTask;
import net.atomcode.bearing.geocoding.QueryGeocodingTask;
import net.atomcode.bearing.geocoding.ReverseGeocodingTask;
import net.atomcode.bearing.location.CurrentLocationTask;

/**
 * Entry class for Bearing library.
 *
 * Use the following fluid API:
 *
 * Current Location
 * =====================
 * Get the users current location
 *
 * Bearing.with(context).locate().accuracy(Accuracy.High).listen({...}).start();
 *
 * The default accuracy is Accuracy.MEDIUM, giving an accuracy to the nearest 50 metres.
 * The Listener callback with return a Location with the latitude,longitude of the latest
 * location with an accuracy better than the given option
 *
 * Geocoding Lookups
 * =====================
 * Make a geocoding lookup in order to convert a Lat,Lng pair into and address
 * or an address string into a Lat,Lng pair
 *
 * Bearing.with(context).geocode("New York, NY").results(5).listen({...}).start();
 *
 * The default number of results is 10, although some requests may get less or no results.
 * The Listener callback will return a List<Address> with a max size of the set results count.
 *
 */
@SuppressWarnings("unused")
public class Bearing
{
	/**
	 * The shared instance to use when
	 */
	static Bearing singleton = null;

	public static boolean isLocationServicesAvailable(Context context)
	{
		if (context == null)
		{
			throw new IllegalArgumentException("Context cannot be null!");
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

	/**
	 * Get the default {@link Bearing} instance
	 * @param context The context in which to make requests
	 */
	public static Bearing with(Context context)
	{
		if (singleton == null)
		{
			singleton = new Bearing(context);
		}
		return singleton;
	}

	final Context context;

	Bearing(Context context)
	{
		this.context = context;
	}

	/**
	 * Geocode the given query into a list of possible resulting lat,lng pairs
	 * @param query The query to check
	 * @return The task to configure and start
	 */
	public GeocodingTask geocode(String query)
	{
		return new QueryGeocodingTask(context, new String[]{query});
	}

	/**
	 * Geocode the given location into a list of possible addresses
	 * @param location The location of the coordinates to look up
	 * @return The task to configure and start
	 */
	public GeocodingTask geocode(Location location)
	{
		return new ReverseGeocodingTask(context, new Double[]{location.getLatitude(), location.getLongitude()});
	}

	/**
	 * Geocode the given location into a list of possible addresses
	 * @param address The address containing the coordinates to look up
	 * @return The task to configure and start
	 */
	public GeocodingTask geocode(Address address)
	{
		return new ReverseGeocodingTask(context, new Double[]{address.getLatitude(), address.getLongitude()});
	}

	/**
	 * Geocode the given location into a list of possible addresses
	 * @param latLng The lat,lng coordinates to look up
	 * @return The task to configure and start
	 */
	public GeocodingTask geocode(LatLng latLng)
	{
		return new ReverseGeocodingTask(context, new Double[]{latLng.latitude, latLng.longitude});
	}

	/**
	 * Locate the current user using the best available method on the device
	 * @return The task to configure and start
	 */
	public CurrentLocationTask locate()
	{
		return new CurrentLocationTask(context);
	}
}
