Bearing
==============

The Bearing library is for simplifying location based requests into a simple, fluent API.
All requests are asynchronous and callback to configured listeners

## Current location

Get the current location of the user

	Bearing.with(context).locate().listen({...}).start();

The current location module also allows the definition of a required accuracy of the request.

	Bearing.with(context).locate().accuracy(Accuracy.HIGH).listen({...}).start();

The default accuracy is MEDIUM which gives the location to the nearest 50m

## Geocoding

To get a list of possible addresses with lat,lng coordinates for a given query

	Bearing.with(context).geocode("New York, NY").listen({...}).start();

Other configurable options include the number of results to return (Default: 10)

	Bearing.with(context).geocode("New York, NY").results(5).listen({...}).start();

## Reverse Geocoding

Bearing also supports reverse geocoding. The action of turning a lat,lng pair into an address

	Location location;
	Bearing.with(context).geocode(location).listen({...}).start();

Bearing supports Reverse Geocoding for the following objects:

	android.location.Location;
	android.location.Address;
	com.google.android.gms.maps.model.LatLng;