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
import android.widget.TextView;

import com.steveq.geometer.gps.DistanceMeterService;
import com.steveq.geometer.obs_pattern.Observer;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements Observer{

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 10;
    public static final int LOCATION_AUTO_START = 20;

    private ServiceConnection mConnection;
    private DistanceMeterService mDistanceMeterService;
    private boolean isBound = false;
    public static boolean isAllowed = false;
    private Intent startServiceIntent;
    public static boolean isRunning = false;
    private static String RUNNING_STATE = "RUNNING_STATE";
    private static String LAST_LATITUDE = "LAST_LATITUDE";
    private static String LAST_LONGITUDE = "LAST_LONGITUDE";

    @BindView(R.id.locateButton) Button mLocateButton;
    @BindView(R.id.stopButton) Button mStopButton;
    @BindView(R.id.latitudeTextView) TextView mLatitudeTextView;
    @BindView(R.id.longitudeTextView) TextView mLongitudeTextView;
    @BindView(R.id.mapButton) Button mMapButton;
    @BindView(R.id.resetButton) Button mResetButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(MainActivity.this);
        requestPermission();
        mLongitudeTextView.setText("0.00");
        mLatitudeTextView.setText("0.00");

        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "Service connected");

                mDistanceMeterService = ((DistanceMeterService.DistanceMeterBinder) service).getDistanceMeterService();
                isBound = true;
                if (mDistanceMeterService != null) {
                    mDistanceMeterService.configureGPS();
                }
                if(mDistanceMeterService.outputJson.exists() && isAllowed){
                    mDistanceMeterService.addObserver(MainActivity.this);
                    mDistanceMeterService.startLocationUpdates();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                isBound = false;
            }
        };

        if(savedInstanceState != null){
            double lastLatitude = savedInstanceState.getDouble(LAST_LATITUDE);
            double lastLongitude = savedInstanceState.getDouble(LAST_LONGITUDE);
            mLatitudeTextView.setText(String.valueOf(lastLatitude));
            mLongitudeTextView.setText(String.valueOf(lastLongitude));
            isRunning = savedInstanceState.getBoolean(RUNNING_STATE);
        }

        mLocateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mDistanceMeterService.isProvider() && isAllowed) {
                    mDistanceMeterService.addObserver(MainActivity.this);
                    mDistanceMeterService.startLocationUpdates(DistanceMeterService.MIN_TIME_BW_UPDATES, DistanceMeterService.MIN_DISTANCE_CHANGE_FOR_UPDATES);
                    isRunning = true;
                } else {
                    showAlert();
                }
            }
        });

        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDistanceMeterService.stopUpdates();
                isRunning = false;
            }
        });

        mMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                intent.putExtra("initial_lat", mDistanceMeterService.getLatitude());
                intent.putExtra("initial_long", mDistanceMeterService.getLongitude());
                startActivityForResult(intent, LOCATION_AUTO_START);
            }
        });

        mResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDistanceMeterService.stopUpdates();
                mDistanceMeterService.deleteHistory();
                mLongitudeTextView.setText("0.00");
                mLatitudeTextView.setText("0.00");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        startServiceIntent = new Intent(this, DistanceMeterService.class);
        bindService(startServiceIntent, mConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == LOCATION_AUTO_START) {
            if (resultCode == RESULT_OK) {
                mDistanceMeterService.startLocationUpdates();
            }
        }
    }

    private void requestPermission() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSION_ACCESS_FINE_LOCATION);
        } else {
            isAllowed = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == MY_PERMISSION_ACCESS_FINE_LOCATION){
            isAllowed = true;
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
    protected void onDestroy() {
        super.onDestroy();
        if (!isRunning) {
            mDistanceMeterService.deleteHistory();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(RUNNING_STATE, isRunning);
        outState.putDouble(LAST_LATITUDE, mDistanceMeterService.mHistory.getLast().getLatitude());
        outState.putDouble(LAST_LONGITUDE, mDistanceMeterService.mHistory.getLast().getLongitude());
    }

    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is turned off")
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(locationIntent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();
    }

    @Override
    public void update(double latitude, double longitude) {
        mLatitudeTextView.setText(String.valueOf(latitude));
        mLongitudeTextView.setText(String.valueOf(longitude));
    }
}
