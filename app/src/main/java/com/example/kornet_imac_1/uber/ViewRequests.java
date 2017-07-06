package com.example.kornet_imac_1.uber;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class ViewRequests extends AppCompatActivity implements LocationListener {
    ListView riderList;
    ArrayList<String> listViewContent;
    ArrayList<String> usernames;
    ArrayList<Double> latitudes;
    ArrayList<Double> longtitudes;
    ArrayAdapter arrayAdapter;
    private LocationManager mLocationManager;
    String provider;
    Location location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_requests);

        riderList = (ListView) findViewById(R.id.riderList);
        listViewContent = new ArrayList<>();
        usernames = new ArrayList<>();
        latitudes = new ArrayList<>();
        longtitudes = new ArrayList<>();

        listViewContent.add("Finding nearby requests..");
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listViewContent);
        riderList.setAdapter(arrayAdapter);


        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        provider = mLocationManager.getBestProvider(new Criteria(), false);
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
        location = mLocationManager.getLastKnownLocation(provider);
        if (location != null) {
            updateLocation();
        }

        riderList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent viewRider = new Intent(getApplicationContext(), ViewRiderLocation.class);
                viewRider.putExtra("username",usernames.get(i));
                viewRider.putExtra("userlatitude",latitudes.get(i));
                viewRider.putExtra("userlongtitude",longtitudes.get(i));
                viewRider.putExtra("driverlatitude", location.getLatitude());
                viewRider.putExtra("driverlongtitude",location.getLongitude());

                startActivity(viewRider);
            }
        });


    }

    public void updateLocation(){
        final ParseGeoPoint userLocation = new ParseGeoPoint(location.getLatitude(), location.getLongitude());

        ParseUser.getCurrentUser().put("location", userLocation);
        ParseUser.getCurrentUser().saveInBackground();

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Requests");
        query.whereDoesNotExist("driverUsername");
        query.whereNotEqualTo("requesterUsername", ParseUser.getCurrentUser().getUsername());
        query.whereNear("requesterLocation", userLocation);
        query.setLimit(10);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    if(objects.size() > 0){
                        listViewContent.clear();
                        usernames.clear();
                        latitudes.clear();
                        longtitudes.clear();

                        for(ParseObject object : objects){
                            Double distanceInKm = userLocation.distanceInKilometersTo((ParseGeoPoint) object.get("requesterLocation"));
                            listViewContent.add(String.valueOf(object.get("requesterUsername")) +" - "+ String.format( "%.1f", distanceInKm )+" KM");
                            usernames.add(object.getString("requesterUsername"));
                            latitudes.add(object.getParseGeoPoint("requesterLocation").getLatitude());
                            longtitudes.add(object.getParseGeoPoint("requesterLocation").getLongitude());
                        }

                        arrayAdapter.notifyDataSetChanged();
                    }
                }
            }
        });
    }
    @Override
    public void onLocationChanged(Location location) {
        updateLocation();
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
        //mLocationManager.removeUpdates(this);
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
