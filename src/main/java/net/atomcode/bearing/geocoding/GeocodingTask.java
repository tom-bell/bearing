package net.atomcode.bearing.geocoding;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Task for geocoding a supplied query into latitude and longitude elements
 */
public class GeocodingTask extends AsyncTask<String, Void, List<Address>>
{
	private static final String WEB_API_URL = "https://maps.googleapis.com/maps/api/geocode/json";

	private Context context;
	private Locale locale;

	private GeocodingTaskListener listener;

	/**
	 * The amount of results to be returned
	 */
	private int resultCount;

	/**
	 * Geocode the supplied request using the devices current locale
	 * @param context The current app context
	 */
	public GeocodingTask(Context context)
	{
		this(context, context.getResources().getConfiguration().locale);
	}

	/**
	 * Geocode the supplied request using the given explicit locale
	 * @param locale The locale to use when geocoding the query
	 */
	public GeocodingTask(Context context, Locale locale)
	{
		this.context = context;
		this.locale = locale;

		// Set a default result count
		this.resultCount = 10;
	}

	/**
	 * Set the listener for this geocoding task
	 * @param listener The listener to use
	 */
	public void setGeocodingTaskListener(GeocodingTaskListener listener)
	{
		this.listener = listener;
	}

	@Override protected List<Address> doInBackground(String... params)
	{
		if (params == null || params.length == 0)
		{
			// No query
			return null;
		}

		String query = params[0];

		if (deviceHasNativeGeocoding())
		{
			return addressForNativeGeocodedQuery(query);
		}
		else
		{
			return addressForRemoteGeocodedQuery(query);
		}
	}

	@Override protected void onPostExecute(List<Address> address)
	{
		super.onPostExecute(address);
		if (address != null)
		{
			if (listener != null)
			{
				listener.onLocationGeocoded(address);
			}
		}
		else
		{
			listener.onLocationGeocodingFailed();
		}
	}

	/**
	 * Check to see if the device has native geocoding capability.
	 * @return {@code true} if ability present, {@code false} otherwise.
	 */
	private boolean deviceHasNativeGeocoding()
	{
		return Geocoder.isPresent();
	}

	/**
	 * Geocode the query natively and return the result.
	 *
	 * Note
	 * =====
	 * Some devices, namely Amazon kindles, will report native geocoding support but
	 * actually not support it. This is caught by a null response. If this occurs
	 * the fallback {@code addressForRemoteGeocodedQuery} will be called
	 *
	 * @param query The query to geocode
	 * @return The geocoded locations
	 */
	private List<Address> addressForNativeGeocodedQuery(String query)
	{
		Geocoder geocoder = new Geocoder(context, locale);

		try
		{
			List<Address> results = geocoder.getFromLocationName(query, resultCount);

			if (results != null && results.size() > 0 && !isCancelled())
			{
				return results;
			}
		}
		catch (IOException ex)
		{
			return addressForRemoteGeocodedQuery(query);
		}

		return null;
	}

	/**
	 * A fallback alternative that will use a web request to geocode the query.
	 *
	 * @param query The query to geocode
	 * @return The geocoded location as returned from the web service.
	 */
	private List<Address> addressForRemoteGeocodedQuery(String query)
	{
		StringBuilder data = new StringBuilder();
		try
		{
			// Make query API compliant
			query = query.replace(" ", "+");
			String params = "?address=" + query + "&sensor=false";

			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(WEB_API_URL + params);
			HttpResponse response;

			if (!isCancelled())
			{
				try
				{
					response = client.execute(request);
				}
				catch (ClientProtocolException ex)
				{
					ex.printStackTrace();
					return null;
				}

				InputStream content = response.getEntity().getContent();

				InputStreamReader inputStreamReader = new InputStreamReader(content);
				BufferedReader reader = new BufferedReader(inputStreamReader);

				String line;
				while ((line = reader.readLine()) != null && !isCancelled())
				{
					data.append(line);
				}
			}
		}
		catch (IOException ex)
		{
			Log.e("Bearing", "Network error connecting to Google Geocoding API" + ex.getMessage());
			return null;
		}

		try
		{
			/*
			{
				"results": [
					{
						"geometry": {
							"location": {
								"lat": <latitude>
								"lng": <longitude>
							}
						}
					}
				]
			}
			 */

			if (!isCancelled())
			{
				JSONObject geocodeData = new JSONObject(data.toString());
				JSONArray addresses = geocodeData.getJSONArray("results");

				List<Address> addressList = new ArrayList<Address>(addresses.length());

				for (int i = 0; i < addresses.length(); i++)
				{
					JSONObject result = addresses.getJSONObject(0);

					JSONObject geometry = result.getJSONObject("geometry");
					JSONObject locationData = geometry.getJSONObject("location");

					Address addr = new Address(locale);
					addr.setLatitude(locationData.getDouble("lat"));
					addr.setLongitude(locationData.getDouble("lng"));

					addressList.add(addr);
				}

				return addressList;
			}
		}
		catch (JSONException ex)
		{
			Log.e("Bearing", "Google Geocoding API format parsing failed! " + ex.getMessage());
		}

		return null;
	}

	/**
	 * Set the desired amount of results to return in the list
	 * @param count The desired amount
	 */
	public void setResultCount(int count)
	{
		resultCount = count;
	}
}
