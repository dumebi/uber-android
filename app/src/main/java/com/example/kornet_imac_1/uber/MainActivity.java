package com.example.kornet_imac_1.uber;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Switch;

import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;


public class MainActivity extends AppCompatActivity {
    Switch riderOrDriverSwitch;
    String riderOrDriver = "rider";
    public void getStarted(View view){
        if(riderOrDriverSwitch.isChecked()){
            riderOrDriver = "driver";
        }
        ParseUser.getCurrentUser().put("riderOrDriver", riderOrDriver);
        ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
            public void done(ParseException e) {
                if (e == null) {
                    redirectUser();

                } else {
                    Log.d("User", "SAVE FAILED " + e.getCause());
                }
            }
        });
    }

    public void redirectUser(){
        if(ParseUser.getCurrentUser().get("riderOrDriver").equals("rider")){
            Intent i = new Intent(getApplicationContext(), UserLocation.class);
            startActivity(i);
        }
        if(ParseUser.getCurrentUser().get("riderOrDriver").equals("driver")){
            Intent i = new Intent(getApplicationContext(), ViewRequests.class);
            startActivity(i);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        riderOrDriverSwitch = (Switch)findViewById(R.id.riderOrDriver);
        getSupportActionBar().hide();

        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("myAppId")
                .clientKey("myMasterKey")
                .server("https://parse-uber.herokuapp.com/parse/")
                .build()
        );

        ParseAnalytics.trackAppOpenedInBackground(getIntent());

        ParseUser.enableAutomaticUser();
        ParseACL defaultACL = new ParseACL();
        // Optionally enable public read access.
        // defaultACL.setPublicReadAccess(true);
        ParseACL.setDefaultACL(defaultACL, true);

        Log.d("current user", String.valueOf(ParseUser.getCurrentUser()));
        ParseUser.getCurrentUser().put("riderOrDriver", "rider");

        if(ParseUser.getCurrentUser() == null){
            ParseAnonymousUtils.logIn(new LogInCallback() {
                @Override
                public void done(ParseUser user, ParseException e) {
                    if (e != null) {
                        Log.d("MyApp", "Anonymous login failed.");
                    } else {
                        Log.d("MyApp", "Anonymous user logged in.");
                    }
                }
            });
        }else{
            if(ParseUser.getCurrentUser().get("riderOrDriver") != null){
                redirectUser();
            }
        }


    }
}
