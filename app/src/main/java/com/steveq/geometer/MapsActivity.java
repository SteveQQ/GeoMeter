package com.steveq.geometer;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.steveq.geometer.gps.DistanceMeterService;
import com.steveq.geometer.obs_pattern.Observer;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, Observer {

    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;
    private static final String TAG = MapsActivity.class.getSimpleName();
    private GoogleMap mMap;

    private ServiceConnection mConnection;
    private DistanceMeterService mDistanceMeterService;
    private boolean isBound = false;
    private Intent startServiceIntent;
    private SupportMapFragment mapFragment;
    private MarkerOptions mTempMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "Service connected");
                mDistanceMeterService = ((DistanceMeterService.DistanceMeterBinder) service).getDistanceMeterService();
                mDistanceMeterService.addObserver(MapsActivity.this);
                isBound = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                isBound = false;
            }
        };

        requestPermission();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(MainActivity.isAllowed) {
            startServiceIntent = new Intent(this, DistanceMeterService.class);
            bindService(startServiceIntent, mConnection, BIND_AUTO_CREATE);
            mapFragment.getMapAsync(MapsActivity.this);
        } else {
            requestPermission();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            unbindService(mConnection);
            mDistanceMeterService.deleteObserver(this);
            Log.d(TAG, "Service unbind");
            isBound = false;
        }
    }

    @Override
    public void onBackPressed() {
        Intent resultIntent = new Intent();
        setResult(RESULT_OK, resultIntent);
        super.onBackPressed();
    }

    private void requestPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSION_ACCESS_FINE_LOCATION);
        } else {
            MainActivity.isAllowed = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == MY_PERMISSION_ACCESS_FINE_LOCATION){
            MainActivity.isAllowed = true;
            mapFragment.getMapAsync(this);
        }
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

        // Add a marker in Sydney and move the camera
        Intent intent = getIntent();
        LatLng initCurrent = new LatLng(intent.getDoubleExtra("initial_lat", 0), intent.getDoubleExtra("initial_long", 0));
        mMap.addMarker(new MarkerOptions().position(initCurrent).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(initCurrent));
        if(mDistanceMeterService.outputJson.exists()){
            mDistanceMeterService.startLocationUpdates();
        }
    }

    @Override
    public void update(double latitude, double longitude) {



        LatLng current = new LatLng(latitude, longitude);
        mMap.addMarker(new MarkerOptions().position(current).title("Current Location"));


        mMap.moveCamera(CameraUpdateFactory.newLatLng(current));
    }
}
