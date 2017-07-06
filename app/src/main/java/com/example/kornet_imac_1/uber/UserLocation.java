package com.example.kornet_imac_1.uber;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserLocation extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private String address;
    private String city;
    private Geocoder geocoder;
    private List<Address> addresses;
    public TextView infoTextView;

    Boolean requestActive = false;
    Button requestUberButton;
    private LocationManager mLocationManager;
    String provider;
    String driverUsername = "";
    ParseGeoPoint driverLocation = new ParseGeoPoint(0,0);
    Handler handler = new Handler();

    public void getUber(View view){
        if(!requestActive) {
            final ParseObject request = new ParseObject("Requests");
            request.put("requesterUsername", ParseUser.getCurrentUser().getUsername());
            ParseACL parseACL = new ParseACL();
            parseACL.setPublicWriteAccess(true);
            parseACL.setPublicReadAccess(true);
            request.setACL(parseACL);

            request.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        infoTextView.setText("Finding Uber driver...");
                        requestUberButton.setText("Cancel Uber");
                        requestActive = true;
                        getLocation();
                    }
                }
            });
        }else{
            infoTextView.setText("Uber Cancelled");
            requestUberButton.setText("Request Uber");
            requestActive = false;

            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Requests");
            query.whereEqualTo("requesterUsername", ParseUser.getCurrentUser().getUsername());
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        if(objects.size() > 0){
                            for(ParseObject object: objects){
                                object.deleteInBackground();
                            }
                        }
                    }
                }
            });
        }
    }

    public void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.clear();

        if(requestActive == false){
            ParseQuery<ParseObject> query = ParseQuery.getQuery("Requests");
            query.whereEqualTo("requesterUsername", ParseUser.getCurrentUser().getUsername());
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        if(objects.size() > 0){
                            for (ParseObject object : objects){
                                requestActive = true;
                                infoTextView.setText("Finding Uber driver...");
                                requestUberButton.setText("Cancel Uber");
                                if(object.get("driverUsername") != null){
                                    driverUsername = object.getString("driverUsername");
                                    infoTextView.setText("Your driver is on their way");
                                    requestUberButton.setVisibility(View.INVISIBLE);
                                    Log.d("AppInfo", driverUsername);
                                }
                            }
                        }
                    }
                }
            });
        }


        Location location = mLocationManager.getLastKnownLocation(provider);
        if (location != null) {
            geocoder = new Geocoder(UserLocation.this, Locale.getDefault());
            try {
                addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                address = addresses.get(0).getAddressLine(0);
                city = addresses.get(0).getLocality();
                if (driverUsername.equals("")){
                    LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 10));
                    mMap.addMarker(new MarkerOptions().position(loc).title(address));
                    mMap.getUiSettings().setMapToolbarEnabled(false);
                }

                if(requestActive){
                    if(!driverUsername.equals("")){
                        ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
                        userQuery.whereEqualTo("username", driverUsername);
                        userQuery.findInBackground(new FindCallback<ParseUser>() {
                            @Override
                            public void done(List<ParseUser> objects, ParseException e) {
                                if (e == null) {
                                    if(objects.size() > 0){
                                        for (ParseUser driver: objects){
                                            driverLocation = driver.getParseGeoPoint("location");
                                        }
                                    }
                                }
                            }
                        });
                        if(driverLocation.getLatitude() != 0 && driverLocation.getLongitude() != 0){
                            Log.d("AppInfo", driverLocation.toString());

                            Double distanceInKm = driverLocation.distanceInKilometersTo(new ParseGeoPoint(location.getLatitude(), location.getLongitude()));
                            if(distanceInKm <= 0){
                                infoTextView.setText("Your driver has arrived");
                            }else{
                                infoTextView.setText("Your driver is "+ String.format( "%.1f", distanceInKm )+" KM away");
                            }

                            ArrayList<Marker> markers = new ArrayList<>();


                            LatLng userloc = new LatLng(location.getLatitude(), location.getLongitude());
                            LatLng driverloc = new LatLng(driverLocation.getLatitude(), driverLocation.getLongitude());

                            markers.add(mMap.addMarker(new MarkerOptions().position(userloc).title("Rider Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))));
                            markers.add(mMap.addMarker(new MarkerOptions().position(driverloc).title("Driver Location")));

                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                            for (Marker marker : markers) {
                                builder.include(marker.getPosition());
                            }
                            LatLngBounds bounds = builder.build();
                            int padding = 100; // offset from edges of the map in pixels
                            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                            mMap.animateCamera(cu);
                            mMap.getUiSettings().setMapToolbarEnabled(false);

                        }
                    }
                    final ParseGeoPoint userLocation = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
                    ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Requests");
                    query.whereEqualTo("requesterUsername", ParseUser.getCurrentUser().getUsername());
                    query.findInBackground(new FindCallback<ParseObject>() {
                        @Override
                        public void done(List<ParseObject> objects, ParseException e) {
                            if (e == null) {
                                if(objects.size() > 0){
                                    for(ParseObject object: objects){
                                        object.put("requesterLocation", userLocation);
                                        object.saveInBackground(new SaveCallback() {
                                            @Override
                                            public void done(ParseException e) {
                                                if (e == null) {
                                                    Log.d("userLocation", "Location saved");
                                                }else{
                                                    Log.d("userLocation", e.getMessage());
                                                }
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getLocation();
            }
        }, 5000);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_location);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        infoTextView = (TextView)findViewById(R.id.infoTextView);
        requestUberButton = (Button)findViewById(R.id.requestUberButton);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        provider = mLocationManager.getBestProvider(new Criteria(), false);
        mLocationManager.requestLocationUpdates(provider, 400, 1, this);

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        getLocation();
    }



    @Override
    public void onLocationChanged(Location location) {
        getLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocationManager.requestLocationUpdates(provider, 400, 1, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationManager.removeUpdates(this);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
