package net.atomcode.bearing.geocoding;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
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
public class ReverseGeocodingTask extends GeocodingTask<Double>
{
	/**
	 * Reverse geocode the supplied request using the devices current locale
	 * @param context The current app context
	 */
	public ReverseGeocodingTask(Context context, Double[]latlng)
	{
		super(context, latlng);
	}

	/**
	 * Reverse geocode the supplied request using the given explicit locale
	 * @param locale The locale to use when geocoding the query
	 */
	public ReverseGeocodingTask(Context context, Double[] latlng, Locale locale)
	{
		super(context, latlng, locale);
	}

	@Override protected List<Address> doInBackground(Double... params)
	{
		if (params == null || params.length < 2)
		{
			Log.w("Bearing", "Invalid lat,lng supplied to ReverseGeocoder");
			return null;
		}

		Double lat = params[0];
		Double lng = params[1];

		if (deviceHasNativeGeocoding())
		{
			return addressForNativeGeocodedQuery(lat, lng);
		}
		else
		{
			return addressForRemoteGeocodedQuery(lat, lng);
		}
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
	 * @param latitude The latitiude of the location to reverse geocode
	 * @param longitude The longitude of the location to reverse geocode
	 * @return The geocoded location
	 */
	private List<Address> addressForNativeGeocodedQuery(Double latitude, Double longitude)
	{
		Geocoder geocoder = new Geocoder(context, locale);

		try
		{
			List<Address> results = geocoder.getFromLocation(latitude, longitude, resultCount);

			if (results != null && results.size() > 0)
			{
				return results;
			}
		}
		catch (IOException ex)
		{
			return addressForRemoteGeocodedQuery(latitude, longitude);
		}

		return null;
	}

	/**
	 * A fallback alternative that will use a web request to geocode the query.
	 *
	 * @param latitude The latitiude of the location to reverse geocode
	 * @param longitude The longitude of the location to reverse geocode
	 * @return The geocoded location as returned from the web service.
	 */
	private List<Address> addressForRemoteGeocodedQuery(Double latitude, Double longitude)
	{
		StringBuilder data = new StringBuilder();
		try
		{
			HttpClient client = new DefaultHttpClient();

			String params = "?latlng=" + latitude + "," + longitude + "&sensor=false";

			HttpGet request = new HttpGet(WEB_API_URL + params);

			HttpResponse response;
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
			while ((line = reader.readLine()) != null)
			{
				data.append(line);
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
			JSONObject geocodeData = new JSONObject(data.toString());

			JSONArray addresses = geocodeData.getJSONArray("results");

			int resultsToRead = Math.min(resultCount, addresses.length());

			List<Address> addressList = new ArrayList<Address>(resultsToRead);
			for (int i = 0; i < resultsToRead; i++)
			{
				JSONObject firstResult = addresses.getJSONObject(0);

				JSONObject geometry = firstResult.getJSONObject("geometry");
				JSONObject locationData = geometry.getJSONObject("location");

				Address result = new Address(locale);
				result.setLatitude(locationData.getDouble("lat"));
				result.setLongitude(locationData.getDouble("lng"));

				JSONArray addressData = firstResult.getJSONArray("address_components");

				for (int addressIndex = 0; addressIndex < addressData.length(); addressIndex++)
				{
					JSONObject addressLine = addressData.getJSONObject(addressIndex);

					String addressLineString = addressLine.getString("long_name");
					result.setAddressLine(addressIndex, addressLineString);

					JSONArray types = addressLine.getJSONArray("types");
					for (int typeIter = 0; typeIter < types.length(); typeIter++)
					{
						String type = types.getString(typeIter);
						if (type.equals("street_number"))
						{
							result.setPremises(addressLineString);
						}
						else if (type.equals("route"))
						{
							result.setSubThoroughfare(addressLineString);
						}
						else if (type.equals("neighborhood"))
						{
							result.setThoroughfare(addressLineString);
						}
						else if (type.equals("sublocality"))
						{
							result.setSubLocality(addressLineString);
						}
						else if (type.equals("administrative_area_level_2"))
						{
							result.setSubAdminArea(addressLineString);
						}
						else if (type.equals("administrative_area_level_1"))
						{
							result.setAdminArea(addressLineString);
						}
						else if (type.equals("country"))
						{
							result.setCountryName(addressLineString);
							result.setCountryCode(addressLine.getString("short_name"));
						}
						else if (type.equals("postal_code"))
						{
							result.setPostalCode(addressLineString);
						}
					}
				}

				addressList.add(result);
			}

			return addressList;
		}
		catch (JSONException ex)
		{
			Log.e("Bearing", "Google Geocoding API format parsing failed! " + ex.getMessage());
		}

		return null;
	}
}
