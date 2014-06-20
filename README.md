Bearing
=======

The Bearing library is for simplifying location based requests into a simple, fluent API.
All requests are asynchronous and callback to configured listeners.

This is a project for the community. All pull requests and bug reports are welcome and encouraged!

## Using bearing in your project

The library is on maven central, and can be included in your gradle project by
adding:

    compile "net.atomcode:bearing:<latest_version>"

Or, if using maven by adding:

    <dependency>
        <groupId>net.atomcode</groupId>
        <artifactId>bearing</artifactId>
        <version>(latest version)</version>
    </dependency>
	
## Android permissions

Currently, in order to use the library your app will need the following permissions in the android manifest:

	<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
	<uses-permission android:name="android.permission.INTERNET"/>
	
The location permission is, naturally for getting the location of the user. Only FINE is required here as FINE implies COARSE.
The internet permission is required for geocoding and some fallback requests.

## Current location

Get the current location of the user

	Bearing.with(context).locate().listen({...}).start();

The current location module also allows the definition of a required accuracy of the request.

	Bearing.with(context).locate().accuracy(Accuracy.HIGH).listen({...}).start();

The default accuracy is MEDIUM which gives the location to the nearest 50m

## Tracking (EXPERIMENTAL)

There is currently experimental support for user tracking

    Bearing.with(context).track().listen({...}).start();

Tracking currently uses a LOT of battery by default, and needs configuring to
use low power sources, and a large step time. i.e.

    Bearing.with(context).track()
        .displacement(1000)
        .rate(60*60*1000)
        .accuracy(Accuracy.LOW)
        .listen({...})
        .start();

Please use this feature with caution it is still a work in progress.

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
