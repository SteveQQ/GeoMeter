package com.steveq.geometer;

import android.Manifest;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.steveq.geometer.gps.DistanceMeterService;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 10;
    private Button mLocateButton;
    private Button mStopButton;
    private ServiceConnection mConnection;
    private DistanceMeterService mDistanceMeterService;
    private boolean isBound;
    private Intent startServiceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLocateButton = (Button) findViewById(R.id.locateButton);
        mStopButton = (Button) findViewById(R.id.stopButton);

        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mDistanceMeterService = ((DistanceMeterService.DistanceMeterBinder)service).getDistanceMeterService();
                Log.d(TAG, "Service connected");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };

        startServiceIntent = new Intent(this, DistanceMeterService.class);

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSION_ACCESS_FINE_LOCATION);
        } else {
            bindService(startServiceIntent, mConnection, BIND_AUTO_CREATE);
        }


        mLocateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mDistanceMeterService.isProvider()) {
                    mDistanceMeterService.startLocationUpdates(DistanceMeterService.MIN_TIME_BW_UPDATES, DistanceMeterService.MIN_DISTANCE_CHANGE_FOR_UPDATES);
                } else {
                    showAlert();
                }
//                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//
//                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
//                            MY_PERMISSION_ACCESS_FINE_LOCATION);
//
//                    return;
//                } else {
//                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, MainActivity.this);
//                }
            }
        });

        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDistanceMeterService.stopUpdates();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == MY_PERMISSION_ACCESS_FINE_LOCATION){
            bindService(startServiceIntent, mConnection, BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
        Log.d(TAG, "Service unbind");
    }



//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        switch (requestCode) {
//            case MY_PERMISSION_ACCESS_FINE_LOCATION:
//                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, this);
//                return;
//        }
//    }
    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is turned off")
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(locationIntent);
                        mDistanceMeterService.configureGPS();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();
    }
}
